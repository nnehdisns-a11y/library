package com.example.ebookmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LibraryDao {

    @Query("SELECT * FROM books")
    abstract fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    abstract suspend fun allBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE treeUri = :treeUri ORDER BY uri")
    abstract suspend fun booksByTree(treeUri: String): List<BookEntity>

    @Upsert
    abstract suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books WHERE treeUri = :treeUri")
    abstract suspend fun deleteBooksByTree(treeUri: String)

    @Query("SELECT * FROM trees ORDER BY addedAt ASC")
    abstract fun observeTrees(): Flow<List<TreeEntity>>

    @Query("SELECT * FROM trees ORDER BY addedAt ASC")
    abstract suspend fun allTrees(): List<TreeEntity>

    @Query("SELECT * FROM trees WHERE uri = :uri")
    abstract suspend fun findTree(uri: String): TreeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTree(tree: TreeEntity)

    @Query("DELETE FROM trees WHERE uri = :uri")
    abstract suspend fun deleteTree(uri: String)

    /** 扫描成功后整体替换某个目录的记录，事务保证不会出现半更新状态。 */
    @Transaction
    open suspend fun replaceTreeScan(treeUri: String, books: List<BookEntity>) {
        deleteBooksByTree(treeUri)
        upsertBooks(books)
    }

    /** 移除授权目录：只删除本地记录，不触碰设备文件。 */
    @Transaction
    open suspend fun removeTreeRecords(treeUri: String) {
        deleteBooksByTree(treeUri)
        deleteTree(treeUri)
    }
}
