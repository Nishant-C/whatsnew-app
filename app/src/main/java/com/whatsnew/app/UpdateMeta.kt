package com.whatsnew.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pure formatting helpers for the "Latest updates" screen (unit-testable). */
object UpdateMeta {
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.US)

    /** "v2.1 • fetched Sep 21, 7:05 PM". */
    fun metaLine(versionName: String?, fetchedAt: Long): String {
        val version = versionName?.ifBlank { null } ?: "?"
        val whenFetched =
            if (fetchedAt > 0) dateFormat.format(Date(fetchedAt)) else "unknown time"
        return "v$version • fetched $whenFetched"
    }
}
