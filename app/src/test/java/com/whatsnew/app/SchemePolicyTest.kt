package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class SchemePolicyTest {

    @Test
    fun `https is allowed`() {
        assertTrue(SchemePolicy.allowLoad("https"))
    }

    @Test
    fun `http is allowed`() {
        assertTrue(SchemePolicy.allowLoad("http"))
    }

    @Test
    fun `intent deep link is swallowed`() {
        assertFalse(SchemePolicy.allowLoad("intent"))
    }

    @Test
    fun `market deep link is swallowed`() {
        assertFalse(SchemePolicy.allowLoad("market"))
    }

    @Test
    fun `null scheme is swallowed`() {
        assertFalse(SchemePolicy.allowLoad(null))
    }
}
