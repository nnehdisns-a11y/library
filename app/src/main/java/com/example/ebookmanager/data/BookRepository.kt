package com.example.ebookmanager.data

import com.example.ebookmanager.data.db.LibraryDao
import com.example.ebookmanager.data.db.TreeEntity
import com.example.ebookmanager.data.db.toBook
import com.example.ebookmanager.data.db.toEntity
import com.example.ebookmanager.data.db.toTreeRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 书库唯一数据入口：所有界面都观察 Room 的 Flow，重启后自动恢复。
 */
class BookRepository(
    private val dao: LibraryDao,
    scope: CoroutineScope,
) {
    val books: StateFlow<List<Book>> = dao.observeBooks()
        .map { entities -> entities.map { it.toBook() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val trees: StateFlow<List<TreeRoot>> = dao.observeTrees()
        .map { entities -> entities.map { it.toTreeRoot() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun addTree(uri: String, displayName: String, path: String) {
        val existing = dao.findTree(uri)
        dao.upsertTree(
            TreeEntity(
                uri = uri,
                displayName = displayName,
                path = path,
                addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            ),
        )
    }

    /**
     * 只在一个目录扫描完整成功后调用：事务内替换该目录的记录，
     * 保留首次加入时间，其他目录不受影响。
     */
    suspend fun syncTree(treeUri: String, scanned: List<Book>) {
        val existingAddedAt = dao.booksByTree(treeUri).associate { it.uri to it.addedAt }
        val entities = scanned.map { book ->
            book.toEntity().copy(
                addedAt = existingAddedAt[book.uri] ?: book.addedAt,
            )
        }
        dao.replaceTreeScan(treeUri, entities)
    }

    /** 移除目录及其书籍记录；不会删除设备上的文件。 */
    suspend fun removeTree(treeUri: String) {
        dao.removeTreeRecords(treeUri)
    }
}
