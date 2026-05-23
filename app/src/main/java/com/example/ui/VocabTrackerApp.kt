package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VocabTrackerApp(viewModel: WordViewModel) {
    // Collect parameters
    val allWords by viewModel.allWords.collectAsStateWithLifecycle()
    val learnedWords by viewModel.learnedWords.collectAsStateWithLifecycle()
    val xp by viewModel.xpState.collectAsStateWithLifecycle()
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    // Form inputs state
    val newWord by viewModel.newWordWord.collectAsStateWithLifecycle()
    val newDef by viewModel.newWordDefinition.collectAsStateWithLifecycle()
    val newEx by viewModel.newWordExample.collectAsStateWithLifecycle()
    val newDiff by viewModel.newWordDifficulty.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Dictionary, 1: My List, 2: Game Mode, 3: Statistics
    var showAddDialog by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var difficultyFilter by remember { mutableStateOf("All") } // "All", "Beginner", "Intermediate", "Advanced"

    // Timer heartbeat for spaced repetition countdown
    var currentHeartbeatTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentHeartbeatTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Material 3 Custom Colors for Design Polish
    val customSlate = Color(0xFF1E293B) // Dark background elements
    val customMint = Color(0xFF10B981) // High-contrast positive mint accent
    val customCoral = Color(0xFFEF4444) // Error/Advanced tag color
    val customAmber = Color(0xFFF59E0B) // Intermediate tag color
    val customBlue = Color(0xFF3B82F6) // Easy tag color
    val gradientPurpleBlue = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF3730A3))
    )

    if (quizState.isActive) {
        // Overlay fully interactive spaced-repetition game screen
        QuizGameScreen(
            state = quizState,
            isDemoMode = viewModel.isDemoMode,
            onAnswerSubmit = { selection -> viewModel.submitAnswer(selection) },
            onNext = { viewModel.goToNextQuestion() },
            onEndGame = { viewModel.endQuizGame() }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Vocab Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "XP Level",
                                    tint = customAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val currentLevel = (xp / 100) + 1
                                Text(
                                    "Word Champion Lvl $currentLevel ($xp XP)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Star, contentDescription = "Education Logo", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.isDemoMode = !viewModel.isDemoMode }) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Toggle Demo Mode",
                                    tint = if (viewModel.isDemoMode) customMint else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    ),
                    modifier = Modifier.testTag("app_top_bar")
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("nav_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Dictionary") },
                        icon = { Icon(Icons.Default.List, contentDescription = "Dictionary Tab") },
                        modifier = Modifier.testTag("nav_tab_dictionary")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("My List") },
                        icon = { Icon(Icons.Default.Star, contentDescription = "My Learned Words Tab") },
                        modifier = Modifier.testTag("nav_tab_mylist")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("Game") },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Spaced Repetition Game Tab") },
                        modifier = Modifier.testTag("nav_tab_game")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        label = { Text("Metrics") },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Progress Statistics Tab") },
                        modifier = Modifier.testTag("nav_tab_metrics")
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add custom learned word") },
                        text = { Text("Add Word") },
                        modifier = Modifier.testTag("add_word_fab"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> DictionaryScreen(
                        allWords = allWords,
                        learnedWords = learnedWords,
                        searchKeyword = searchKeyword,
                        onSearchChange = { searchKeyword = it },
                        difficultyFilter = difficultyFilter,
                        onDifficultyFilterChange = { difficultyFilter = it },
                        onMarkLearned = { word -> viewModel.markWordAsLearned(word) },
                        onUnlearnWord = { word -> viewModel.unlearnWord(word) }
                    )
                    1 -> MyListScreen(
                        learnedWords = learnedWords,
                        heartbeatTime = currentHeartbeatTime,
                        onUnlearn = { word -> viewModel.unlearnWord(word) },
                        onDelete = { word -> viewModel.deleteWordPermanently(word) }
                    )
                    2 -> GameModeScreen(
                        learnedWords = learnedWords,
                        heartbeatTime = currentHeartbeatTime,
                        isDemoMode = viewModel.isDemoMode,
                        onStartGame = { onlyDue -> viewModel.startQuizGame(onlyDue) }
                    )
                    3 -> ProgressMetricsScreen(
                        allWords = allWords,
                        learnedWords = learnedWords,
                        totalXp = xp
                    )
                }
            }
        }
    }

    // Add Word Dialog
    if (showAddDialog) {
        var localError by remember { mutableStateOf<String?>(null) }

        // Automatically trigger AI word meaning lookup after typing pause of 1.2 seconds!
        LaunchedEffect(newWord) {
            val query = newWord.trim()
            if (query.length >= 2 && newDef.trim().isEmpty()) {
                delay(1200L) // Debounce typing
                if (newWord.trim() == query && newDef.trim().isEmpty()) {
                    viewModel.autoLookupWord(query) { error ->
                        localError = error
                    }
                }
            }
        }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_word_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Record Learned Word",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newWord,
                        onValueChange = { viewModel.updateWordInput(it, newDef, newEx, newDiff) },
                        label = { Text("Word") },
                        placeholder = { Text("e.g., Ephemeral") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_word_text"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Gemini Loading / Auto-Fill feedback bar
                    if (isAiLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "✨ Gemini is fetching description...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (newWord.trim().length >= 2) {
                        // Quick click-to-trigger or re-trigger helper
                        TextButton(
                            onClick = {
                                localError = null
                                viewModel.autoLookupWord(newWord.trim()) { error ->
                                    localError = error
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(bottom = 12.dp)
                        ) {
                            Text("✨ AI Auto-Fill Meaning")
                        }
                    }

                    OutlinedTextField(
                        value = newDef,
                        onValueChange = { viewModel.updateWordInput(newWord, it, newEx, newDiff) },
                        label = { Text("Definition") },
                        placeholder = { Text("e.g., Lasting for a very short time.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("input_word_definition"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = newEx,
                        onValueChange = { viewModel.updateWordInput(newWord, newDef, it, newDiff) },
                        label = { Text("Example Sentence") },
                        placeholder = { Text("e.g., Sunset colors are ephemeral...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("input_word_example"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    // Difficulty selector
                    Text(
                        "Difficulty Tag",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { valTag ->
                            val isSelected = valTag == newDiff
                            val chipBg = if (isSelected) {
                                when (valTag) {
                                    "Beginner" -> customBlue
                                    "Intermediate" -> customAmber
                                    else -> customCoral
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipBg)
                                    .clickable {
                                        viewModel.updateWordInput(newWord, newDef, newEx, valTag)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    valTag,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = chipText
                                    )
                                )
                            }
                        }
                    }

                    if (localError != null) {
                        Text(
                            localError!!,
                            color = customCoral,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.saveCustomWord(
                                    onSuccess = {
                                        showAddDialog = false
                                        localError = null
                                    },
                                    onError = { err ->
                                        localError = err
                                    }
                                )
                            },
                            modifier = Modifier.testTag("submit_word_button")
                        ) {
                            Text("Record Word")
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// SCREEN: DICTIONARY
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    allWords: List<WordEntity>,
    learnedWords: List<WordEntity>,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    difficultyFilter: String,
    onDifficultyFilterChange: (String) -> Unit,
    onMarkLearned: (WordEntity) -> Unit,
    onUnlearnWord: (WordEntity) -> Unit
) {
    // Soft semantic colors
    val customMint = Color(0xFF10B981)
    val customBlue = Color(0xFF3B82F6)
    val customAmber = Color(0xFFF59E0B)
    val customCoral = Color(0xFFEF4444)

    val filteredList = allWords.filter { word ->
        val matchesKeyword = word.word.contains(searchKeyword, ignoreCase = true) ||
                word.definition.contains(searchKeyword, ignoreCase = true)
        val matchesDifficulty = difficultyFilter == "All" || word.difficulty.equals(difficultyFilter, ignoreCase = true)
        matchesKeyword && matchesDifficulty
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search text field
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = onSearchChange,
            placeholder = { Text("Search words or definitions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("dictionary_search"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            trailingIcon = if (searchKeyword.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                    }
                }
            } else null
        )

        // Difficulty Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Beginner", "Intermediate", "Advanced").forEach { option ->
                val isSelected = option == difficultyFilter
                val count = if (option == "All") allWords.size else allWords.count { it.difficulty.equals(option, ignoreCase = true) }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onDifficultyFilterChange(option) },
                    label = { Text("$option ($count)") },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "No Results",
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        "No vocab found matching criteria.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("dictionary_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { word ->
                    val isWordLearned = learnedWords.any { it.word.equals(word.word, ignoreCase = true) }

                    // Card styled uniquely
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("word_card_${word.word}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        word.word,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (word.isCustom) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text(
                                                "Custom",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }

                                // Difficulty badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (word.difficulty) {
                                                "Beginner" -> customBlue.copy(alpha = 0.15f)
                                                "Intermediate" -> customAmber.copy(alpha = 0.15f)
                                                else -> customCoral.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        word.difficulty,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = when (word.difficulty) {
                                                "Beginner" -> customBlue
                                                "Intermediate" -> customAmber
                                                else -> customCoral
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                word.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (word.example.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            // Draw subtle vertical indicator on the left
                                            drawRect(
                                                color = Color.LightGray,
                                                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                                                size = androidx.compose.ui.geometry.Size(
                                                    4.dp.toPx(),
                                                    size.height
                                                )
                                            )
                                        }
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        "\"${word.example}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (isWordLearned) {
                                    Button(
                                        onClick = { onUnlearnWord(word) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = customMint.copy(alpha = 0.15f),
                                            contentColor = customMint
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Active learning marker",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Learned", style = MaterialTheme.typography.labelMedium)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onMarkLearned(word) }
                                    ) {
                                        Text("Start Learning", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// SCREEN: MY LEARNED WORDS LIST
// -----------------------------------------------------
@Composable
fun MyListScreen(
    learnedWords: List<WordEntity>,
    heartbeatTime: Long,
    onUnlearn: (WordEntity) -> Unit,
    onDelete: (WordEntity) -> Unit
) {
    val customMint = Color(0xFF10B981)
    val customBlue = Color(0xFF3B82F6)
    val customAmber = Color(0xFFF59E0B)
    val customCoral = Color(0xFFEF4444)

    if (learnedWords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "No items",
                    modifier = Modifier
                        .size(96.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Text(
                    "Your Review List is empty!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Browse words in the Dictionary tab and tap 'Start Learning', or add custom vocabulary to kick off your spaced-repetition process.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "My Learning Backlog (${learnedWords.size} words)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("learned_list")
            ) {
                items(learnedWords) { word ->
                    // Calculate review timer
                    val timeDiff = word.nextReviewTimestamp - heartbeatTime
                    val isDue = timeDiff <= 0

                    val dueString = if (isDue) {
                        "Due Now"
                    } else {
                        val totalSecs = timeDiff / 1000L
                        if (totalSecs < 60) {
                            "Due in ${totalSecs}s"
                        } else if (totalSecs < 3600) {
                            "Due in ${totalSecs / 60}m"
                        } else {
                            "Due in ${totalSecs / 3600}h"
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("learned_word_${word.word}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDue) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isDue) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Title & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        word.word,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    // Leitner Box level
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Box Tier",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Leitner Box ${word.boxLevel}/5",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                // Due countdown banner
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isDue) customMint.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        dueString,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDue) customMint else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                word.definition,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            // Score Ratio Metrics bar
                            val totalAnswers = word.correctCount + word.incorrectCount
                            val ratioPercent = if (totalAnswers > 0) {
                                (word.correctCount * 100) / totalAnswers
                            } else 100

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Accuracy",
                                        tint = customMint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Accuracy: $ratioPercent% (${word.correctCount}/${totalAnswers})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Row {
                                    // Remove learning status but keep in dictionary
                                    IconButton(
                                        onClick = { onUnlearn(word) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Stop learning this word",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    // If custom word, allow deleting permanently
                                    if (word.isCustom) {
                                        IconButton(
                                            onClick = { onDelete(word) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Permanently delete custom word",
                                                tint = customCoral,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// SCREEN: REVIEW GAME SELECTOR
// -----------------------------------------------------
@Composable
fun GameModeScreen(
    learnedWords: List<WordEntity>,
    heartbeatTime: Long,
    isDemoMode: Boolean,
    onStartGame: (Boolean) -> Unit
) {
    val customMint = Color(0xFF10B981)
    val customAmber = Color(0xFFF59E0B)

    val dueWordsCount = learnedWords.count { it.nextReviewTimestamp <= heartbeatTime }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Hero Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Game Play Icon",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Leitner Spaced Repetition Game",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Spaced repetition leverages expanding intervals to store words in long-term memory. Overwrite review schedules as you get answers right, or cycle back when you fail.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Due Banner Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (dueWordsCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (dueWordsCount > 0) customMint else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "$dueWordsCount Words Due For Review",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (dueWordsCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (dueWordsCount > 0) "Perfect timing! Complete scheduled audits." else "All caught up! Next reviews countdown in background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = { onStartGame(true) },
                enabled = dueWordsCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp)
                    .testTag("start_due_quiz"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Review Scheduled Words ($dueWordsCount)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { onStartGame(false) },
                enabled = learnedWords.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp)
                    .testTag("start_practice_quiz"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Review All Learned Words (${learnedWords.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (learnedWords.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Add words to your learned vocabulary registry first.",
                    color = customAmber,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

// -----------------------------------------------------
// GAME REVIEW: FULL SCREEN OVERLAY
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGameScreen(
    state: WordViewModel.QuizState,
    isDemoMode: Boolean,
    onAnswerSubmit: (Int) -> Unit,
    onNext: () -> Unit,
    onEndGame: () -> Unit
) {
    if (state.questions.isEmpty()) return

    val customMint = Color(0xFF10B981)
    val customCoral = Color(0xFFEF4444)

    if (state.completed) {
        // SUMMARY REVEAL SCREEN
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_complete_card"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Victory Medal",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        "Session Cleared!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // SCORE METRIC ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Accuracy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${state.sessionCorrectCount} / ${state.questions.size}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("XP Earned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "+${state.sessionXpEarned} XP",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = customMint)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "You did amazing! Spaced intervals have been updated securely inside your Leitner system database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onEndGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quiz_finish_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Return to Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    } else {
        // GAMEPLAY SCRIPTS
        val currentIdx = state.currentQuestionIndex
        val question = state.questions[currentIdx]
        val word = question.word

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Review Game",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onEndGame) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Exit game")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                )
            }
        ) { scaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Progressbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Word ${currentIdx + 1} of ${state.questions.size}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "Current Box: ${word.boxLevel}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    )
                }

                LinearProgressIndicator(
                    progress = (currentIdx + 1).toFloat() / state.questions.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .padding(bottom = 24.dp)
                )

                // Large central word question card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Identify the correct definition for:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            word.word,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-1).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Word Level: ${word.difficulty}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // 4 Choices List
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    question.options.forEachIndexed { optIdx, optionText ->
                        val isUserSelection = state.selectedOptionIndex == optIdx
                        val isCorrectOption = question.correctOptionIndex == optIdx

                        val containerColor = when {
                            !state.hasAnswered -> MaterialTheme.colorScheme.surface
                            isCorrectOption -> customMint.copy(alpha = 0.15f)
                            isUserSelection -> customCoral.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        }

                        val borderColor = when {
                            !state.hasAnswered -> {
                                if (isUserSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            }
                            isCorrectOption -> customMint
                            isUserSelection -> customCoral
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        }

                        val textColor = when {
                            !state.hasAnswered -> MaterialTheme.colorScheme.onSurface
                            isCorrectOption -> customMint
                            isUserSelection -> customCoral
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }

                        Card(
                            onClick = { if (!state.hasAnswered) onAnswerSubmit(optIdx) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_option_$optIdx"),
                            enabled = !state.hasAnswered
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic letter symbol (A, B, C, D)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (state.hasAnswered && (isCorrectOption || isUserSelection)) {
                                                if (isCorrectOption) customMint else customCoral
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.hasAnswered && (isCorrectOption || isUserSelection)) {
                                        Icon(
                                            if (isCorrectOption) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = "Status",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            "${'A' + optIdx}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    optionText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCorrectOption && state.hasAnswered) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Answer reveal helper bottom panel
                if (state.hasAnswered) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isCorrect) customMint.copy(alpha = 0.08f) else customCoral.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, if (state.isCorrect) customMint.copy(alpha = 0.2f) else customCoral.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (state.isCorrect) "Fantastic! +10 XP" else "Incorrect (+2 XP)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isCorrect) customMint else customCoral
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Definition: ${word.definition}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (word.example.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "\"${word.example}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNext,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .testTag("quiz_continue_button")
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------
// SCREEN: PROGRESS METRICS & CANVAS VISUALIZATIONS
// -----------------------------------------------------
@Composable
fun ProgressMetricsScreen(
    allWords: List<WordEntity>,
    learnedWords: List<WordEntity>,
    totalXp: Int
) {
    val customMint = Color(0xFF10B981)
    val customBlue = Color(0xFF3B82F6)
    val customAmber = Color(0xFFF59E0B)
    val customCoral = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Progress Insights",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // XP LEVEL BANNER
        val level = (totalXp / 100) + 1
        val pointsNextLevel = 100 - (totalXp % 100)
        val levelPercent = (totalXp % 100) / 100f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$level",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Vocabulary Level $level",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "$pointsNextLevel XP to Level ${level + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = levelPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // STATIC SUMMARY INFO-CARDS GRID (2x2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "Total Learned",
                value = "${learnedWords.size}",
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Dictionary",
                value = "${allWords.size}",
                icon = Icons.Default.List,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val dueCount = learnedWords.count { it.nextReviewTimestamp <= System.currentTimeMillis() }
            MetricCard(
                title = "Due Reviews",
                value = "$dueCount",
                icon = Icons.Default.Refresh,
                tintColor = if (dueCount > 0) customMint else Color.Gray,
                modifier = Modifier.weight(1f)
            )
            val correctTotal = learnedWords.sumOf { it.correctCount }
            val wrongTotal = learnedWords.sumOf { it.incorrectCount }
            val ratio = if (correctTotal + wrongTotal > 0) {
                (correctTotal * 100) / (correctTotal + wrongTotal)
            } else 100
            MetricCard(
                title = "Avg Accuracy",
                value = "$ratio%",
                icon = Icons.Default.Check,
                tintColor = customMint,
                modifier = Modifier.weight(1f)
            )
        }

        // CANVAS GRAPHIC: PIE CHART / RING CHART FOR LEARNED PROGRESS
        Text(
            "Overall Mastery Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val ratioPercent = if (allWords.isNotEmpty()) {
                    (learnedWords.size.toFloat() / allWords.size.toFloat())
                } else 0f

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeColor = MaterialTheme.colorScheme.primaryContainer
                    val progressColor = MaterialTheme.colorScheme.primary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // draw background circle
                        drawCircle(
                            color = strokeColor,
                            radius = size.minDimension / 2.2f,
                            style = Stroke(width = 16.dp.toPx())
                        )
                        // draw active progress arc
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = 360f * ratioPercent,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx())
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(ratioPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            text = "Learned",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, text = "Learned (${learnedWords.size})")
                    LegendItem(color = MaterialTheme.colorScheme.primaryContainer, text = "Locked (${allWords.size - learnedWords.size})")
                }
            }
        }

        // CANVAS GRAPHIC: LEITNER BOX CHRONICLE
        Text(
            "Leitner Box Distribution",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val box1Count = learnedWords.count { it.boxLevel == 1 }
                val box2Count = learnedWords.count { it.boxLevel == 2 }
                val box3Count = learnedWords.count { it.boxLevel == 3 }
                val box4Count = learnedWords.count { it.boxLevel == 4 }
                val box5Count = learnedWords.count { it.boxLevel == 5 }
                val boxCounts = listOf(box1Count, box2Count, box3Count, box4Count, box5Count)
                val maxCount = maxOf(1, boxCounts.maxOrNull() ?: 1)

                // Render dynamic beautiful bars
                for (i in 1..5) {
                    val count = boxCounts[i - 1]
                    val pct = count.toFloat() / maxCount.toFloat()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Box $i",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(pct)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        when (i) {
                                            1 -> customCoral
                                            2 -> customAmber
                                            3 -> customBlue
                                            4 -> MaterialTheme.colorScheme.primary
                                            else -> customMint
                                        }
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tintColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
