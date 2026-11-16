package com.example.ebookmanager.data

import android.net.Uri
import android.provider.DocumentsContract

/**
 * SAF 不提供真实文件系统路径，这里把 tree documentId 还原成可读路径。
 * 仅对 local/primary provider 有效，其他 provider 回退为 documentId。
 */
object SafPaths {
    fun rootLabel(treeUri: Uri): String {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return treeUri.toString()
        val path = documentId.substringAfter(':', "")
        return if (documentId.startsWith("primary:")) {
            if (path.isEmpty()) "内部存储" else "内部存储/$path"
        } else {
            documentId
        }
    }

    fun fullPath(book: Book): String = "${rootLabel(Uri.parse(book.treeUri))}/${book.relativePath}"
}
