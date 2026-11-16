package com.example.ebookmanager.core

/** SAF 持久化授权是否仍然有效。 */
object SafAuthorization {
    fun isAuthorized(treeUri: String, persistedTreeUris: Set<String>): Boolean =
        treeUri in persistedTreeUris
}
