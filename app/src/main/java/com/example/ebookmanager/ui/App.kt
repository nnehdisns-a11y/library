package com.example.ebookmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.ui.detail.BookDetailScreen
import com.example.ebookmanager.ui.home.HomeScreen
import com.example.ebookmanager.ui.library.LibraryScreen
import com.example.ebookmanager.ui.scan.ScanScreen

sealed interface Screen {
    data object Home : Screen
    data object Library : Screen
    data object Scan : Screen
    data class Detail(val book: Book) : Screen
}

@Composable
fun EbookApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen !is Screen.Home) {
        screen = when (screen) {
            is Screen.Detail -> Screen.Library
            else -> Screen.Home
        }
    }

    when (val current = screen) {
        Screen.Home -> HomeScreen(
            onOpenLibrary = { screen = Screen.Library },
            onScan = { screen = Screen.Scan },
        )

        Screen.Library -> LibraryScreen(
            onBack = { screen = Screen.Home },
            onScan = { screen = Screen.Scan },
            onOpenBook = { screen = Screen.Detail(it) },
        )

        Screen.Scan -> ScanScreen(
            onBack = { screen = Screen.Home },
            onOpenLibrary = { screen = Screen.Library },
        )

        is Screen.Detail -> BookDetailScreen(
            book = current.book,
            onBack = { screen = Screen.Library },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("返回") }
                    }
                },
            )
        },
        content = content,
    )
}
