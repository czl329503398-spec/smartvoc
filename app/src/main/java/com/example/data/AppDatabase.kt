package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocab_tracker_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.wordDao())
                }
            }
        }

        private suspend fun populateDatabase(wordDao: WordDao) {
            val seedWords = listOf(
                // Beginner / Easy
                WordEntity(
                    word = "Diligent",
                    definition = "Showing care and conscientiousness in one's work or duties.",
                    example = "The diligent student studied chemistry every night and earned top marks.",
                    difficulty = "Beginner",
                    isCustom = false
                ),
                WordEntity(
                    word = "Elated",
                    definition = "Ecstatically happy, proud, or in high spirits.",
                    example = "He was absolutely elated when his team won the championship match.",
                    difficulty = "Beginner",
                    isCustom = false
                ),
                WordEntity(
                    word = "Ample",
                    definition = "Enough or more than enough; plentiful or abundant.",
                    example = "There is ample pantry food and drinking water for the weekend camping trip.",
                    difficulty = "Beginner",
                    isCustom = false
                ),
                WordEntity(
                    word = "Candid",
                    definition = "Truthful and straightforward; frank and honest.",
                    example = "During the interview, the actor gave a candid reflection on his early failures.",
                    difficulty = "Beginner",
                    isCustom = false
                ),
                WordEntity(
                    word = "Naive",
                    definition = "Showing a lack of experience, wisdom, or judgment.",
                    example = "It was naive of him to think that renting this expensive apartment would be simple.",
                    difficulty = "Beginner",
                    isCustom = false
                ),
                // Intermediate / Medium
                WordEntity(
                    word = "Benevolent",
                    definition = "Well-meaning and kindly; serving a charitable purpose.",
                    example = "The benevolent organization donated millions of dollars to local public hospitals.",
                    difficulty = "Intermediate",
                    isCustom = false
                ),
                WordEntity(
                    word = "Pragmatic",
                    definition = "Dealing with things sensibly and realistically based on practical factors.",
                    example = "Our lead designer took a pragmatic approach to cutting manufacturing costs safely.",
                    difficulty = "Intermediate",
                    isCustom = false
                ),
                WordEntity(
                    word = "Resilient",
                    definition = "Able to withstand or recover quickly from difficult conditions.",
                    example = "These resilient trees survived the harsh winter storm and bloomed in spring.",
                    difficulty = "Intermediate",
                    isCustom = false
                ),
                WordEntity(
                    word = "Vivid",
                    definition = "Producing powerful, detailed, and strong clear mental images.",
                    example = "She painted a vivid childhood memory of running through fields of gold sunflowers.",
                    difficulty = "Intermediate",
                    isCustom = false
                ),
                WordEntity(
                    word = "Scrutinize",
                    definition = "To examine or inspect closely and thoroughly.",
                    example = "The auditor began to scrutinize the financial books for any hidden errors.",
                    difficulty = "Intermediate",
                    isCustom = false
                ),
                // Advanced / Hard
                WordEntity(
                    word = "Ephemeral",
                    definition = "Lasting for a very short time; fleeting or transient.",
                    example = "The spectacular colors of the sunset were ephemeral, disappearing in minutes.",
                    difficulty = "Advanced",
                    isCustom = false
                ),
                WordEntity(
                    word = "Capricious",
                    definition = "Given to sudden and unaccountable changes of mood or behavior.",
                    example = "The high altitude weather is notoriously capricious, shifting from sun to storm.",
                    difficulty = "Advanced",
                    isCustom = false
                ),
                WordEntity(
                    word = "Loquacious",
                    definition = "Tending to talk a great deal; extremely talkative.",
                    example = "The loquacious guide shared countless fascinating historical details about the castle.",
                    difficulty = "Advanced",
                    isCustom = false
                ),
                WordEntity(
                    word = "Pernicious",
                    definition = "Having a harmful effect, especially in a gradual, subtle, or sneaky way.",
                    example = "Unchecked toxic habits can have a pernicious effect on domestic happiness.",
                    difficulty = "Advanced",
                    isCustom = false
                ),
                WordEntity(
                    word = "Superfluous",
                    definition = "Unnecessary, redundant, or more than enough.",
                    example = "Adding more text to this beautifully precise summary would be superfluous.",
                    difficulty = "Advanced",
                    isCustom = false
                ),
                WordEntity(
                    word = "Ubiquitous",
                    definition = "Present, appearing, or found everywhere.",
                    example = "The internet and smart mobile devices have become ubiquitous in daily life.",
                    difficulty = "Advanced",
                    isCustom = false
                )
            )
            seedWords.forEach { wordDao.insertWord(it) }
        }
    }
}
