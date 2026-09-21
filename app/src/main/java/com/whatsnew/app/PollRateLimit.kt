package com.whatsnew.app

/**
 * Pure helper: decides whether a repeated background failure is worth writing
 * to the activity log. The watcher polls every two seconds, so an unchecked
 * fault would flood the log; this rate-limits those entries.
 */
object PollRateLimit {
    /**
     * True when at least [minIntervalMs] has passed since [lastLogAt], meaning
     * another occurrence may be logged.
     */
    fun shouldLogFailure(now: Long, lastLogAt: Long, minIntervalMs: Long = 600_000L): Boolean =
        now - lastLogAt >= minIntervalMs
}
