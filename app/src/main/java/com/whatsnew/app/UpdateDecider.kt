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

    /**
     * What to do when an app is opened, under the prefetch design:
     * - version changed (or first sighting) -> fetch quietly in the
     *   background. No popup now: the dialog appears on a later open, from
     *   stored data, so there is never a fetch-then-popup lag.
     * - version unchanged -> show the stored changelog immediately if it
     *   hasn't been shown yet.
     */
    fun decideOnOpen(
        ignored: Boolean,
        storedVersion: Long,
        currentVersion: Long,
        storedChangelog: String?,
        shownChangelog: String?
    ): OpenAction {
        if (ignored) return OpenAction.Nothing
        if (shouldFetch(false, storedVersion, currentVersion)) return OpenAction.FetchInBackground
        if (shouldShowPopup(storedChangelog, shownChangelog)) {
            return OpenAction.ShowStored(storedChangelog!!)
        }
        return OpenAction.Nothing
    }
}

/** The three things an app open can lead to. */
sealed interface OpenAction {
    /** Version changed: refresh the stored changelog silently. */
    data object FetchInBackground : OpenAction
    /** Have fetched info the user hasn't seen: pop the dialog instantly. */
    data class ShowStored(val changelog: String) : OpenAction
    /** Nothing to do. */
    data object Nothing : OpenAction
}

/**
 * Pure decision logic for changelog fetch retries.
 *
 * A failed fetch (page didn't load, timed out, no section found) is retried the
 * next time the app is opened, up to [MAX_ATTEMPTS]; then the app is added to
 * the ignore list so system apps without a Play Store listing don't refetch
 * forever. The user can remove it from the ignore list to retry later.
 */
object FetchRetry {
    const val MAX_ATTEMPTS = 3

    enum class Outcome { RETRY, GIVE_UP }

    /** [previousFailures] is how many times this exact app version already failed. */
    fun onFailure(previousFailures: Int): Outcome =
        if (previousFailures + 1 >= MAX_ATTEMPTS) Outcome.GIVE_UP else Outcome.RETRY
}
