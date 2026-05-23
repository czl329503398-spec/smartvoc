package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.WordEntity
import com.example.data.WordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class WordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    
    // UI state Flows
    val allWords: StateFlow<List<WordEntity>>
    val learnedWords: StateFlow<List<WordEntity>>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = WordRepository(database.wordDao())
        
        allWords = repository.allWords
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        learnedWords = repository.learnedWords
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Settings
    var isDemoMode = true // Fast intervals in minutes if true, standard in days if false

    // XP system
    private val _xpState = MutableStateFlow(0)
    val xpState: StateFlow<Int> = _xpState.asStateFlow()

    fun addXp(amount: Int) {
        _xpState.value += amount
    }

    // Custom Word Form Inputs
    private val _newWordWord = MutableStateFlow("")
    val newWordWord = _newWordWord.asStateFlow()

    private val _newWordDefinition = MutableStateFlow("")
    val newWordDefinition = _newWordDefinition.asStateFlow()

    private val _newWordExample = MutableStateFlow("")
    val newWordExample = _newWordExample.asStateFlow()

    private val _newWordDifficulty = MutableStateFlow("Intermediate") // Beginner, Intermediate, Advanced
    val newWordDifficulty = _newWordDifficulty.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    fun autoLookupWord(wordQuery: String, onComplete: (String?) -> Unit = {}) {
        if (wordQuery.trim().isEmpty()) return
        viewModelScope.launch {
            _isAiLoading.value = true
            val result = com.example.data.GeminiService.lookUpWord(wordQuery)
            if (result != null) {
                _newWordDefinition.value = result.definition
                _newWordExample.value = result.example
                // Normalize difficulty tag to match standard tags
                val rawDiff = result.difficulty.trim()
                val normalDiff = when {
                    rawDiff.contains("Beginner", ignoreCase = true) || rawDiff.contains("Easy", ignoreCase = true) -> "Beginner"
                    rawDiff.contains("Advanced", ignoreCase = true) || rawDiff.contains("Hard", ignoreCase = true) -> "Advanced"
                    else -> "Intermediate"
                }
                _newWordDifficulty.value = normalDiff
                onComplete(null)
            } else {
                onComplete("AI Lookup failed. Please check your network or enter manually.")
            }
            _isAiLoading.value = false
        }
    }

    fun updateWordInput(word: String, definition: String, example: String, difficulty: String) {
        _newWordWord.value = word
        _newWordDefinition.value = definition
        _newWordExample.value = example
        _newWordDifficulty.value = difficulty
    }

    fun saveCustomWord(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val wordTxt = _newWordWord.value.trim()
        val defTxt = _newWordDefinition.value.trim()
        val exTxt = _newWordExample.value.trim()
        val diff = _newWordDifficulty.value

        if (wordTxt.isEmpty()) {
            onError("Word text cannot be empty.")
            return
        }
        if (defTxt.isEmpty()) {
            onError("Definition cannot be empty.")
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newWord = WordEntity(
                word = wordTxt,
                definition = defTxt,
                example = exTxt,
                difficulty = diff,
                isCustom = true,
                isLearned = true,
                boxLevel = 1,
                nextReviewTimestamp = now, // Review immediately upon creation
                lastReviewedTimestamp = 0
            )
            repository.insertWord(newWord)
            // Reset inputs
            _newWordWord.value = ""
            _newWordDefinition.value = ""
            _newWordExample.value = ""
            _newWordDifficulty.value = "Intermediate"
            
            addXp(20) // Award 20 XP for adding a word!
            onSuccess()
        }
    }

    fun markWordAsLearned(word: WordEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.markAsLearned(word.id, now)
            addXp(15) // Award 15 XP for marking a word learned!
        }
    }

    fun unlearnWord(word: WordEntity) {
        viewModelScope.launch {
            val resetWord = word.copy(
                isLearned = false,
                boxLevel = 1,
                nextReviewTimestamp = 0L,
                lastReviewedTimestamp = 0L,
                correctCount = 0,
                incorrectCount = 0
            )
            repository.updateWord(resetWord)
        }
    }

    fun deleteWordPermanently(word: WordEntity) {
        viewModelScope.launch {
            repository.deleteWord(word)
        }
    }

    // GAME REVIEW STATE MACHINE
    data class QuizQuestion(
        val word: WordEntity,
        val options: List<String>, // list of shuffled definition options
        val correctOptionIndex: Int
    )

    data class QuizState(
        val isActive: Boolean = false,
        val questions: List<QuizQuestion> = emptyList(),
        val currentQuestionIndex: Int = 0,
        val selectedOptionIndex: Int? = null,
        val hasAnswered: Boolean = false,
        val isCorrect: Boolean = false,
        val sessionCorrectCount: Int = 0,
        val sessionXpEarned: Int = 0,
        val completed: Boolean = false
    )

    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    fun startQuizGame(onlyDue: Boolean) {
        val now = System.currentTimeMillis()
        val learnedList = learnedWords.value
        val allWordsList = allWords.value

        if (learnedList.isEmpty()) {
            return
        }

        // Filter words for this quiz session. Either only items due under Leitner rules, or all learned lists
        val targetReviewList = if (onlyDue) {
            learnedList.filter { it.nextReviewTimestamp <= now }
        } else {
            learnedList
        }.shuffled().take(10) // up to 10 words per quiz game

        if (targetReviewList.isEmpty()) {
            return
        }

        // Generate Quiz questions with distractors
        val quizQuestions = targetReviewList.map { targetWord ->
            // Distractors are definitions of other words in the DB
            val otherDefinitions = allWordsList
                .filter { it.word != targetWord.word }
                .map { it.definition }
                .shuffled()
                .take(3)

            // Make sure we have 4 options
            val options = (otherDefinitions + targetWord.definition).shuffled()
            val correctIndex = options.indexOf(targetWord.definition)

            QuizQuestion(
                word = targetWord,
                options = options,
                correctOptionIndex = correctIndex
            )
        }

        _quizState.value = QuizState(
            isActive = true,
            questions = quizQuestions,
            currentQuestionIndex = 0,
            selectedOptionIndex = null,
            hasAnswered = false,
            sessionCorrectCount = 0,
            sessionXpEarned = 0,
            completed = false
        )
    }

    fun submitAnswer(optionIndex: Int) {
        val currentQuiz = _quizState.value
        if (!currentQuiz.isActive || currentQuiz.hasAnswered || currentQuiz.completed) return

        val currentQuestion = currentQuiz.questions[currentQuiz.currentQuestionIndex]
        val isCorrect = optionIndex == currentQuestion.correctOptionIndex
        val word = currentQuestion.word

        viewModelScope.launch {
            // Leitner formula
            val nextLevel = if (isCorrect) {
                minOf(5, word.boxLevel + 1)
            } else {
                1 // reset on error
            }

            // Interval in milliseconds
            val addIntervalMs: Long = if (isDemoMode) {
                // Short minutes intervals for rich live demos:
                // Box 1: 1 min, Box 2: 3 min, Box 3: 7 min, Box 4: 15 min, Box 5: 30 min
                when (nextLevel) {
                    1 -> 1 * 60 * 1000L
                    2 -> 3 * 60 * 1000L
                    3 -> 7 * 60 * 1000L
                    4 -> 15 * 60 * 1000L
                    else -> 30 * 60 * 1000L
                }
            } else {
                // Real Leitner intervals (Days)
                // Box 1: 1 day, Box 2: 3 days, Box 3: 7 days, Box 4: 14 days, Box 5: 30 days
                when (nextLevel) {
                    1 -> 1 * 24 * 60 * 60 * 1000L
                    2 -> 3 * 24 * 60 * 60 * 1000L
                    3 -> 7 * 24 * 60 * 60 * 1000L
                    4 -> 14 * 24 * 60 * 60 * 1000L
                    else -> 30 * 24 * 60 * 60 * 1000L
                }
            }

            val now = System.currentTimeMillis()
            val updatedWord = word.copy(
                boxLevel = nextLevel,
                nextReviewTimestamp = now + addIntervalMs,
                lastReviewedTimestamp = now,
                correctCount = word.correctCount + if (isCorrect) 1 else 0,
                incorrectCount = word.incorrectCount + if (!isCorrect) 1 else 0
            )

            // Save back to db
            repository.updateWord(updatedWord)

            // XP
            val xpGain = if (isCorrect) 10 else 2
            addXp(xpGain)

            // Update local game state
            _quizState.value = currentQuiz.copy(
                selectedOptionIndex = optionIndex,
                hasAnswered = true,
                isCorrect = isCorrect,
                sessionCorrectCount = currentQuiz.sessionCorrectCount + if (isCorrect) 1 else 0,
                sessionXpEarned = currentQuiz.sessionXpEarned + xpGain
            )
        }
    }

    fun goToNextQuestion() {
        val currentQuiz = _quizState.value
        if (!currentQuiz.isActive || !currentQuiz.hasAnswered) return

        val nextIndex = currentQuiz.currentQuestionIndex + 1
        if (nextIndex >= currentQuiz.questions.size) {
            // Quiz completed!
            _quizState.value = currentQuiz.copy(
                completed = true
            )
        } else {
            _quizState.value = currentQuiz.copy(
                currentQuestionIndex = nextIndex,
                selectedOptionIndex = null,
                hasAnswered = false
            )
        }
    }

    fun endQuizGame() {
        _quizState.value = QuizState(isActive = false)
    }

    // Factory to help instantiate VM with application parameter
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WordViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return WordViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
