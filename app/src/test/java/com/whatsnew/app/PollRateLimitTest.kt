package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class PollRateLimitTest {

    @Test
    fun `logs when the interval has fully passed`() {
        assertTrue(PollRateLimit.shouldLogFailure(600_000L, 0L))
    }

    @Test
    fun `stays silent inside the interval`() {
        assertFalse(PollRateLimit.shouldLogFailure(599_999L, 0L))
    }

    @Test
    fun `stays silent with no time elapsed`() {
        assertFalse(PollRateLimit.shouldLogFailure(1_000L, 1_000L))
    }

    @Test
    fun `custom interval is respected`() {
        assertFalse(PollRateLimit.shouldLogFailure(5_000L, 0L, minIntervalMs = 10_000L))
        assertTrue(PollRateLimit.shouldLogFailure(10_000L, 0L, minIntervalMs = 10_000L))
    }
}
