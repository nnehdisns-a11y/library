package com.example.ebookmanager.data

import android.content.Context
import android.net.Uri

/** 保存用户授权的目录树 URI（权限本身由系统持久化）。 */
class TreeStore(context: Context) {
    private val prefs = context.getSharedPreferences("ebook_trees", Context.MODE_PRIVATE)

    fun roots(): List<Uri> = prefs.getStringSet(KEY_ROOTS, emptySet())
        .orEmpty()
        .map { Uri.parse(it) }
        .sortedBy { it.toString() }

    fun add(uri: Uri) {
        val updated = prefs.getStringSet(KEY_ROOTS, emptySet()).orEmpty().toMutableSet()
        updated.add(uri.toString())
        prefs.edit().putStringSet(KEY_ROOTS, updated).apply()
    }

    fun remove(uri: Uri) {
        val updated = prefs.getStringSet(KEY_ROOTS, emptySet()).orEmpty().toMutableSet()
        updated.remove(uri.toString())
        prefs.edit().putStringSet(KEY_ROOTS, updated).apply()
    }

    private companion object {
        const val KEY_ROOTS = "tree_uris"
    }
}
