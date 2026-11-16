package com.example.ebookmanager.data

import com.example.ebookmanager.core.EbookFormat

/**
 * 纯 Kotlin 数据模型，不依赖 Android，方便做 JVM 单元测试。
 */
data class Book(
    val uri: String,
    val treeUri: String,
    val displayName: String,
    val relativePath: String,
    val format: EbookFormat,
    val size: Long,
    val lastModified: Long,
    val addedAt: Long,
) {
    val title: String
        get() = displayName.substringBeforeLast('.', displayName)

    val folder: String
        get() = relativePath.substringBeforeLast('/', "").ifEmpty { "/" }
}
