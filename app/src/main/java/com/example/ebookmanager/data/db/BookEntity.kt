package com.example.ebookmanager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index(value = ["treeUri"])],
)
data class BookEntity(
    @PrimaryKey val uri: String,
    val treeUri: String,
    val displayName: String,
    val title: String,
    val folder: String,
    val relativePath: String,
    val format: String,
    val size: Long,
    val lastModified: Long,
    val addedAt: Long,
)
