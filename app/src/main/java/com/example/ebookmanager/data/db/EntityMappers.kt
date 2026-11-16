package com.example.ebookmanager.data.db

import com.example.ebookmanager.core.EbookFormat
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.data.TreeRoot

fun BookEntity.toBook(): Book = Book(
    uri = uri,
    treeUri = treeUri,
    displayName = displayName,
    relativePath = relativePath,
    format = runCatching { EbookFormat.valueOf(format) }.getOrNull()
        ?: EbookFormat.fromFileName(displayName)
        ?: EbookFormat.EPUB,
    size = size,
    lastModified = lastModified,
    addedAt = addedAt,
)

fun Book.toEntity(): BookEntity = BookEntity(
    uri = uri,
    treeUri = treeUri,
    displayName = displayName,
    title = title,
    folder = folder,
    relativePath = relativePath,
    format = format.name,
    size = size,
    lastModified = lastModified,
    addedAt = addedAt,
)

fun TreeEntity.toTreeRoot(): TreeRoot = TreeRoot(
    uri = uri,
    displayName = displayName,
    path = path,
    addedAt = addedAt,
)
