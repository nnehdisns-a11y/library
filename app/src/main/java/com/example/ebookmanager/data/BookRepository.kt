package com.example.ebookmanager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 阶段一用内存仓库，进程结束后需要重新扫描；阶段二会替换为 Room。
 */
object BookRepository {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    /** 按 document URI 去重合并；重新扫描不会产生重复记录。 */
    fun merge(scanned: List<Book>) {
        _books.update { current ->
            val byUri = current.associateBy { it.uri }.toMutableMap()
            scanned.forEach { book ->
                val existing = byUri[book.uri]
                byUri[book.uri] = if (existing == null) {
                    book
                } else {
                    book.copy(addedAt = existing.addedAt)
                }
            }
            byUri.values.sortedBy { it.title.lowercase() }
        }
    }

    fun removeTree(treeUri: String) {
        _books.update { list -> list.filterNot { it.treeUri == treeUri } }
    }

    fun clear() {
        _books.value = emptyList()
    }
}
