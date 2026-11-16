package com.example.ebookmanager.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafAuthorizationTest {

    @Test
    fun `目录仍在系统授权列表时判定有效`() {
        assertTrue(
            SafAuthorization.isAuthorized(
                treeUri = "content://tree/a",
                persistedTreeUris = setOf("content://tree/a", "content://tree/b"),
            ),
        )
    }

    @Test
    fun `用户撤销授权后判定失效`() {
        assertFalse(
            SafAuthorization.isAuthorized(
                treeUri = "content://tree/a",
                persistedTreeUris = setOf("content://tree/b"),
            ),
        )
        assertFalse(SafAuthorization.isAuthorized("content://tree/a", emptySet()))
    }
}
