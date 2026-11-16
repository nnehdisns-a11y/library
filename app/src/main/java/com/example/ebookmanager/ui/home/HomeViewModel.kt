package com.example.ebookmanager.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.ebookmanager.EbookApp
import com.example.ebookmanager.data.Book
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val books: StateFlow<List<Book>> = (application as EbookApp).bookRepository.books
}
