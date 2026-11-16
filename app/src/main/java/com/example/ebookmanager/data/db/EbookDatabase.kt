package com.example.ebookmanager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, TreeEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EbookDatabase : RoomDatabase() {

    abstract fun libraryDao(): LibraryDao

    companion object {
        @Volatile
        private var instance: EbookDatabase? = null

        fun get(context: Context): EbookDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EbookDatabase::class.java,
                    "ebook-library.db",
                ).build().also { instance = it }
            }
    }
}
