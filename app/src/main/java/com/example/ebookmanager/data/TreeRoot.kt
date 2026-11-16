package com.example.ebookmanager.data

/**
 * 授权目录的持久化信息。是否仍然有效由系统 persistedUriPermissions 实时判断。
 */
data class TreeRoot(
    val uri: String,
    val displayName: String,
    val path: String,
    val addedAt: Long,
)
