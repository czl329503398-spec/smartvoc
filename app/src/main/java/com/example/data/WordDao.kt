package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY word ASC")
    fun getAllWordsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE isLearned = 1")
    fun getLearnedWordsFlow(): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): WordEntity?

    @Query("UPDATE words SET isLearned = 1, boxLevel = 1, nextReviewTimestamp = :now, lastReviewedTimestamp = 0 WHERE id = :id")
    suspend fun markAsLearned(id: Int, now: Long)

    @Query("SELECT * FROM words WHERE isLearned = 1 AND nextReviewTimestamp <= :now")
    suspend fun getDueReviewWords(now: Long): List<WordEntity>
}
