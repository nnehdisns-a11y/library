package com.example.ebookmanager.data

import com.example.ebookmanager.core.EbookFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryQueryTest {

    private val treeA = "content://tree/a"
    private val treeB = "content://tree/b"

    private val books = listOf(
        book("三体.epub", "小说/科幻", EbookFormat.EPUB, 2_000_000, 100, treeA),
        book("Dune.mobi", "小说/科幻", EbookFormat.MOBI, 3_000_000, 300, treeA),
        book("The Three-Body Problem.pdf", "Books", EbookFormat.PDF, 5_000_000, 200, treeB),
        book("沙丘.azw3", "小说/科幻", EbookFormat.AZW3, 1_000_000, 400, treeB),
        book("读书笔记.txt", "笔记", EbookFormat.TXT, 10_000, 500, treeA),
    )

    @Test
    fun `空条件返回全部并按文件名排序`() {
        val result = LibraryQuery.filterAndSort(books, LibraryFilterState())

        assertEquals(5, result.size)
        assertEquals("Dune", result.first().title)
        assertEquals("读书笔记", result.last().title)
    }

    @Test
    fun `按中文书名搜索`() {
        val result = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(query = "三体"),
        )

        assertEquals(1, result.size)
        assertEquals("三体", result.first().title)
    }

    @Test
    fun `英文搜索不区分大小写`() {
        val lower = LibraryQuery.filterAndSort(books, LibraryFilterState(query = "dune"))
        val upper = LibraryQuery.filterAndSort(books, LibraryFilterState(query = "DUNE"))

        assertEquals(1, lower.size)
        assertEquals(1, upper.size)
        assertEquals(lower.first().uri, upper.first().uri)
    }

    @Test
    fun `可以按原始文件名搜索`() {
        val result = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(query = "Three-Body Problem.pdf"),
        )

        assertEquals(1, result.size)
        assertEquals("The Three-Body Problem", result.first().title)
    }

    @Test
    fun `可以按文件夹名称或路径搜索`() {
        val byName = LibraryQuery.filterAndSort(books, LibraryFilterState(query = "科幻"))
        val byPath = LibraryQuery.filterAndSort(books, LibraryFilterState(query = "小说/科幻"))

        assertEquals(3, byName.size)
        assertEquals(3, byPath.size)
    }

    @Test
    fun `格式筛选与关键词同时生效`() {
        val pdf = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(query = "three", format = EbookFormat.PDF),
        )
        val noMatch = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(query = "dune", format = EbookFormat.PDF),
        )

        assertEquals(1, pdf.size)
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `文件夹筛选只返回同一授权目录下的记录`() {
        val result = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(folder = FolderKey(treeA, "小说/科幻")),
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.treeUri == treeA })
        assertTrue(result.none { it.displayName == "沙丘.azw3" })
    }

    @Test
    fun `清空条件恢复完整书库`() {
        val filtered = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(
                query = "dune",
                format = EbookFormat.MOBI,
                folder = FolderKey(treeA, "小说/科幻"),
            ),
        )
        val reset = LibraryQuery.filterAndSort(books, LibraryFilterState())

        assertEquals(1, filtered.size)
        assertEquals(5, reset.size)
    }

    @Test
    fun `按文件大小从大到小排序`() {
        val result = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(sort = BookSort.SIZE),
        )

        assertEquals(
            listOf("The Three-Body Problem", "Dune", "三体", "沙丘", "读书笔记"),
            result.map { it.title },
        )
    }

    @Test
    fun `按最近修改时间从新到旧排序`() {
        val result = LibraryQuery.filterAndSort(
            books,
            LibraryFilterState(sort = BookSort.MODIFIED),
        )

        assertEquals("读书笔记", result.first().title)
        assertEquals("三体", result.last().title)
    }

    @Test
    fun `文件夹汇总包含数量并区分授权目录`() {
        val folders = LibraryQuery.folders(books)

        assertEquals(4, folders.size)
        assertEquals(2, folders.first { it.key == FolderKey(treeA, "小说/科幻") }.count)
        assertEquals(1, folders.first { it.key == FolderKey(treeB, "小说/科幻") }.count)
        assertEquals(1, folders.first { it.key == FolderKey(treeB, "Books") }.count)
    }

    private fun book(
        name: String,
        folder: String,
        format: EbookFormat,
        size: Long,
        modified: Long,
        treeUri: String,
    ): Book {
        val relative = if (folder == "/") name else "$folder/$name"
        return Book(
            uri = "content://doc/$treeUri/$relative",
            treeUri = treeUri,
            displayName = name,
            relativePath = relative,
            format = format,
            size = size,
            lastModified = modified,
            addedAt = 0,
        )
    }
}
