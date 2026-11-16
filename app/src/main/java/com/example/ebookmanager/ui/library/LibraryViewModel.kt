package com.example.ebookmanager.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ebookmanager.EbookApp
import com.example.ebookmanager.core.EbookFormat
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.data.BookSort
import com.example.ebookmanager.data.FolderKey
import com.example.ebookmanager.data.LibraryFilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as EbookApp).bookRepository

    val books: StateFlow<List<Book>> = repository.books

    private val _filters = MutableStateFlow(LibraryFilterState())
    val filters: StateFlow<LibraryFilterState> = _filters.asStateFlow()

    fun setQuery(value: String) = _filters.update { it.copy(query = value) }

    fun setFormat(value: EbookFormat?) = _filters.update { it.copy(format = value) }

    fun setFolder(value: FolderKey?) = _filters.update { it.copy(folder = value) }

    fun setSort(value: BookSort) = _filters.update { it.copy(sort = value) }

    fun clearAll() {
        _filters.value = LibraryFilterState()
    }
}
