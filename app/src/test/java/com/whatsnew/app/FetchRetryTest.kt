package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class FetchRetryTest {

    @Test
    fun `first failure retries`() {
        assertEquals(FetchRetry.Outcome.RETRY, FetchRetry.onFailure(0))
    }

    @Test
    fun `second failure retries`() {
        assertEquals(FetchRetry.Outcome.RETRY, FetchRetry.onFailure(1))
    }

    @Test
    fun `third consecutive failure gives up`() {
        assertEquals(FetchRetry.Outcome.GIVE_UP, FetchRetry.onFailure(2))
    }

    @Test
    fun `further failures stay given up`() {
        assertEquals(FetchRetry.Outcome.GIVE_UP, FetchRetry.onFailure(10))
    }

    @Test
    fun `max attempts is three`() {
        assertEquals(3, FetchRetry.MAX_ATTEMPTS)
    }
}
