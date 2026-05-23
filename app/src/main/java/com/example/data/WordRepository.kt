package com.example.data

import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {
    val allWords: Flow<List<WordEntity>> = wordDao.getAllWordsFlow()
    val learnedWords: Flow<List<WordEntity>> = wordDao.getLearnedWordsFlow()

    suspend fun insertWord(word: WordEntity): Long {
        return wordDao.insertWord(word)
    }

    suspend fun updateWord(word: WordEntity) {
        wordDao.updateWord(word)
    }

    suspend fun deleteWord(word: WordEntity) {
        wordDao.deleteWord(word)
    }

    suspend fun markAsLearned(id: Int, now: Long) {
        wordDao.markAsLearned(id, now)
    }

    suspend fun getDueReviewWords(now: Long): List<WordEntity> {
        return wordDao.getDueReviewWords(now)
    }
}
