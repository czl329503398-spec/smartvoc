package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val definition: String,
    val example: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val isCustom: Boolean, // true if entered by user, false if seeded/built-in
    val isLearned: Boolean = false, // true if user has started learning/tracking this word
    val boxLevel: Int = 1, // Leitner box level (1 to 5)
    val nextReviewTimestamp: Long = 0L, // timestamp when it should be reviewed next
    val lastReviewedTimestamp: Long = 0L, // timestamp when it was last reviewed
    val correctCount: Int = 0,
    val incorrectCount: Int = 0
) : Serializable
