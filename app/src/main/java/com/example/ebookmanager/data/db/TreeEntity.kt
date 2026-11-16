package com.example.ebookmanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trees")
data class TreeEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val path: String,
    val addedAt: Long,
)
