package com.example.ebookmanager.data

import com.example.ebookmanager.core.EbookFormat

/** 文件所在文件夹的身份：同一个相对路径可能出现在不同授权目录下。 */
data class FolderKey(
    val treeUri: String,
    val relativeDir: String,
)

enum class BookSort(val label: String, val hint: String) {
    NAME("文件名", "A → Z"),
    SIZE("文件大小", "大 → 小"),
    MODIFIED("最近修改", "新 → 旧"),
}

data class LibraryFilterState(
    val query: String = "",
    val format: EbookFormat? = null,
    val folder: FolderKey? = null,
    val sort: BookSort = BookSort.NAME,
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || format != null || folder != null
}

data class FolderSummary(
    val key: FolderKey,
    val count: Int,
)

/**
 * 纯内存过滤，不访问磁盘、不触发扫描。
 */
object LibraryQuery {

    fun filterAndSort(books: List<Book>, state: LibraryFilterState): List<Book> {
        val keyword = state.query.trim()
        val format = state.format
        val folder = state.folder

        return books.asSequence()
            .filter { keyword.isEmpty() || it.matches(keyword) }
            .filter { format == null || it.format == format }
            .filter {
                folder == null ||
                    (it.treeUri == folder.treeUri && it.folder == folder.relativeDir)
            }
            .sortedWith(comparator(state.sort))
            .toList()
    }

    fun folders(books: List<Book>): List<FolderSummary> =
        books.groupBy { FolderKey(it.treeUri, it.folder) }
            .map { (key, items) -> FolderSummary(key, items.size) }
            .sortedBy { it.key.relativeDir.lowercase() }

    private fun Book.matches(keyword: String): Boolean =
        title.contains(keyword, ignoreCase = true) ||
            displayName.contains(keyword, ignoreCase = true) ||
            folder.contains(keyword, ignoreCase = true) ||
            relativePath.contains(keyword, ignoreCase = true)

    private fun comparator(sort: BookSort): Comparator<Book> = when (sort) {
        BookSort.NAME -> compareBy { it.title.lowercase() }
        BookSort.SIZE -> compareByDescending<Book> { it.size }
            .thenBy { it.title.lowercase() }
        BookSort.MODIFIED -> compareByDescending<Book> { it.lastModified }
            .thenBy { it.title.lowercase() }
    }
}
