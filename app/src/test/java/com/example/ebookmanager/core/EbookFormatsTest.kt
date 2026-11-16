package com.example.ebookmanager.core

import com.example.ebookmanager.data.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EbookFormatsTest {

    @Test
    fun `识别支持的电子书格式`() {
        assertEquals(EbookFormat.EPUB, EbookFormat.fromFileName("三体.epub"))
        assertEquals(EbookFormat.PDF, EbookFormat.fromFileName("book.PDF"))
        assertEquals(EbookFormat.TXT, EbookFormat.fromFileName("readme.txt"))
        assertEquals(EbookFormat.AZW3, EbookFormat.fromFileName("a.azw3"))
        assertEquals(EbookFormat.CBR, EbookFormat.fromFileName("comic.cbr"))
    }

    @Test
    fun `忽略普通文件`() {
        assertNull(EbookFormat.fromFileName("论文.docx"))
        assertNull(EbookFormat.fromFileName("README"))
        assertNull(EbookFormat.fromFileName("archive.epub.zip"))
        assertNull(EbookFormat.fromFileName(""))
    }

    @Test
    fun `从文件名生成书名和文件夹`() {
        val book = Book(
            uri = "content://test/book",
            treeUri = "content://test/tree",
            displayName = "三体.epub",
            relativePath = "小说/科幻/三体.epub",
            format = EbookFormat.EPUB,
            size = 2048,
            lastModified = 0,
            addedAt = 0,
        )

        assertEquals("三体", book.title)
        assertEquals("小说/科幻", book.folder)
    }
}
