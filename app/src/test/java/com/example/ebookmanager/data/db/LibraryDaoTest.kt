package com.example.ebookmanager.data.db

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryDaoTest {

    private lateinit var context: Context
    private lateinit var db: EbookDatabase
    private lateinit var dao: LibraryDao

    private val treeA = "content://tree/a"
    private val treeB = "content://tree/b"

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EbookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.libraryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `书籍记录可以正常写入和读取`() = runBlocking {
        dao.upsertBooks(listOf(book("content://doc/a1", "三体.epub", treeA, size = 2048)))

        val saved = dao.allBooks()
        assertEquals(1, saved.size)
        assertEquals("三体.epub", saved.first().displayName)
        assertEquals("三体", saved.first().title)
        assertEquals("EPUB", saved.first().format)
        assertEquals(2048L, saved.first().size)
    }

    @Test
    fun `相同 URI 不会产生重复记录`() = runBlocking {
        val entity = book("content://doc/a1", "三体.epub", treeA)
        dao.upsertBooks(listOf(entity))
        dao.upsertBooks(listOf(entity))

        assertEquals(1, dao.allBooks().size)
    }

    @Test
    fun `相同 URI 再次扫描时可以更新记录`() = runBlocking {
        dao.upsertBooks(listOf(book("content://doc/a1", "三体.epub", treeA, size = 100)))
        dao.upsertBooks(
            listOf(book("content://doc/a1", "三体（修订版）.epub", treeA, size = 200)),
        )

        val saved = dao.allBooks()
        assertEquals(1, saved.size)
        assertEquals(200L, saved.first().size)
        assertEquals("三体（修订版）.epub", saved.first().displayName)
    }

    @Test
    fun `多个授权目录的数据能够分别保存`() = runBlocking {
        dao.upsertBooks(
            listOf(
                book("content://doc/a1", "A.epub", treeA),
                book("content://doc/b1", "B.pdf", treeB),
            ),
        )

        assertEquals(1, dao.booksByTree(treeA).size)
        assertEquals(1, dao.booksByTree(treeB).size)
        assertEquals(2, dao.allBooks().size)
    }

    @Test
    fun `删除一个目录的记录不会影响其他目录`() = runBlocking {
        dao.upsertTree(tree(treeA, "目录A"))
        dao.upsertTree(tree(treeB, "目录B"))
        dao.upsertBooks(
            listOf(
                book("content://doc/a1", "A.epub", treeA),
                book("content://doc/b1", "B.pdf", treeB),
            ),
        )

        dao.removeTreeRecords(treeA)

        assertTrue(dao.booksByTree(treeA).isEmpty())
        assertEquals(1, dao.booksByTree(treeB).size)
        assertNull(dao.findTree(treeA))
        assertEquals(1, dao.allTrees().size)
    }

    @Test
    fun `App 重启后可以恢复书库数据`() {
        val name = "restart-test.db"
        context.deleteDatabase(name)

        val first = Room.databaseBuilder(context, EbookDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            first.libraryDao().upsertTree(tree(treeA, "目录A"))
            first.libraryDao().upsertBooks(listOf(book("content://doc/a1", "三体.epub", treeA)))
        }
        first.close()

        val second = Room.databaseBuilder(context, EbookDatabase::class.java, name)
            .allowMainThreadQueries()
            .build()
        val books = runBlocking { second.libraryDao().allBooks() }
        val trees = runBlocking { second.libraryDao().allTrees() }
        second.close()
        context.deleteDatabase(name)

        assertEquals(1, books.size)
        assertEquals("三体.epub", books.first().displayName)
        assertEquals(1, trees.size)
    }

    @Test
    fun `同步只替换目标目录，未完成的扫描保留旧数据`() = runBlocking {
        dao.upsertBooks(
            listOf(
                book("content://doc/a1", "旧书1.epub", treeA),
                book("content://doc/a2", "旧书2.epub", treeA),
                book("content://doc/b1", "B.pdf", treeB),
            ),
        )

        // 扫描取消或失败时不会调用 replaceTreeScan，旧数据必须保持完整
        assertEquals(3, dao.allBooks().size)

        dao.replaceTreeScan(treeA, listOf(book("content://doc/a3", "新书.epub", treeA)))

        val all = dao.allBooks()
        assertEquals(2, all.size)
        assertEquals(
            listOf("新书.epub"),
            all.filter { it.treeUri == treeA }.map { it.displayName }.sorted(),
        )
        assertEquals(1, all.count { it.treeUri == treeB })
    }

    private fun book(
        uri: String,
        name: String,
        treeUri: String,
        size: Long = 100,
    ) = BookEntity(
        uri = uri,
        treeUri = treeUri,
        displayName = name,
        title = name.substringBeforeLast('.'),
        folder = "小说",
        relativePath = "小说/$name",
        format = "EPUB",
        size = size,
        lastModified = 0,
        addedAt = 0,
    )

    private fun tree(uri: String, name: String) = TreeEntity(
        uri = uri,
        displayName = name,
        path = "内部存储/$name",
        addedAt = 0,
    )
}
