package com.example.ebookmanager

import android.app.Application
import com.example.ebookmanager.data.BookRepository
import com.example.ebookmanager.data.db.EbookDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class EbookApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: EbookDatabase by lazy { EbookDatabase.get(this) }

    val bookRepository: BookRepository by lazy {
        BookRepository(database.libraryDao(), appScope)
    }
}
