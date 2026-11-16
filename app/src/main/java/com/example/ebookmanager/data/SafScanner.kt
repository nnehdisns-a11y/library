package com.example.ebookmanager.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import com.example.ebookmanager.core.EbookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class TreeScanResult(
    val books: List<Book>,
    val dirs: Int,
)

/**
 * 用 DocumentsContract 直接查询子项，比 DocumentFile 递归快很多。
 * 只读取文件信息，不修改任何文件。
 */
class SafScanner(private val resolver: ContentResolver) {

    suspend fun scan(
        treeUri: Uri,
        onProgress: (dirs: Int, books: Int) -> Unit,
    ): TreeScanResult = withContext(Dispatchers.IO) {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val queue = ArrayDeque<DirEntry>()
        queue.addLast(DirEntry(rootDocumentId, ""))

        val visitedDirs = HashSet<String>()
        val seenUris = HashSet<String>()
        val books = ArrayList<Book>()
        var dirs = 0

        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()

            val dir = queue.removeFirst()
            if (!visitedDirs.add(dir.documentId)) continue
            dirs++

            for (child in queryChildren(treeUri, dir.documentId)) {
                if (child.isDirectory) {
                    queue.addLast(DirEntry(child.documentId, join(dir.relativePath, child.name)))
                    continue
                }

                val format = EbookFormat.fromFileName(child.name) ?: continue
                // TXT 无法区分普通文本，过滤掉明显不是书的超小文本文件
                if (format == EbookFormat.TXT && child.size in 1L until MIN_TXT_BYTES) continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.documentId)
                if (!seenUris.add(documentUri.toString())) continue

                books += Book(
                    uri = documentUri.toString(),
                    treeUri = treeUri.toString(),
                    displayName = child.name,
                    relativePath = join(dir.relativePath, child.name),
                    format = format,
                    size = child.size,
                    lastModified = child.lastModified,
                    addedAt = System.currentTimeMillis(),
                )
            }

            if (dirs % PROGRESS_EVERY_DIRS == 0) {
                onProgress(dirs, books.size)
            }
        }

        onProgress(dirs, books.size)
        TreeScanResult(books = books, dirs = dirs)
    }

    private fun queryChildren(treeUri: Uri, parentDocumentId: String): List<ChildDoc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
        val result = ArrayList<ChildDoc>()
        try {
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex) ?: continue
                    val name = cursor.getString(nameIndex) ?: continue
                    val mime = cursor.getString(mimeIndex)
                    val size = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                    val modified = if (cursor.isNull(modifiedIndex)) 0L else cursor.getLong(modifiedIndex)
                    result += ChildDoc(
                        documentId = documentId,
                        name = name,
                        isDirectory = mime == Document.MIME_TYPE_DIR,
                        size = size,
                        lastModified = modified,
                    )
                }
            }
        } catch (e: Exception) {
            // 个别目录可能无权限或 provider 异常，跳过该目录，不中断整次扫描
        }
        return result
    }

    private fun join(parent: String, name: String): String =
        if (parent.isEmpty()) name else "$parent/$name"

    private data class DirEntry(
        val documentId: String,
        val relativePath: String,
    )

    private data class ChildDoc(
        val documentId: String,
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
    )

    private companion object {
        const val MIN_TXT_BYTES = 1024L
        const val PROGRESS_EVERY_DIRS = 10
    }
}
