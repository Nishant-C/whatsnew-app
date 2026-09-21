package com.whatsnew.app

/** Pure decision logic for the "when do we fetch / when do we pop up" rules. */
object UpdateDecider {

    /**
     * True when we should go fetch the Play Store changelog:
     * the app is not ignored AND (we've never seen it before OR its
     * versionCode changed since we last recorded it).
     *
     * A first sighting fetches too: the app is new to the watcher, so showing
     * its current changelog once is useful and proves the watcher is working.
     */
    fun shouldFetch(ignored: Boolean, storedVersion: Long, currentVersion: Long): Boolean {
        if (ignored) return false
        if (storedVersion == -1L) return true
        return currentVersion != storedVersion
    }

    /**
     * True when the popup should be shown for freshly fetched text:
     * text is non-blank AND different from the last text we showed for this app.
     */
    fun shouldShowPopup(fetched: String?, lastShown: String?): Boolean {
        if (fetched.isNullOrBlank()) return false
        return fetched.trim() != lastShown?.trim()
    }
}

/**
 * Pure decision logic for changelog fetch retries.
 *
 * A failed fetch (page didn't load, timed out, no section found) is retried the
 * next time the app is opened, up to [MAX_ATTEMPTS]; then we give up on that
 * version so system apps without a Play Store listing don't refetch forever.
 */
object FetchRetry {
    const val MAX_ATTEMPTS = 3

    enum class Outcome { RETRY, GIVE_UP }

    /** [previousFailures] is how many times this exact app version already failed. */
    fun onFailure(previousFailures: Int): Outcome =
        if (previousFailures + 1 >= MAX_ATTEMPTS) Outcome.GIVE_UP else Outcome.RETRY
}
