package com.whatsnew.app

import android.content.Context

/** Simple SharedPreferences store: ignore list + per-app version/changelog baselines. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("whatsnew", Context.MODE_PRIVATE)

    var monitoringEnabled: Boolean
        get() = sp.getBoolean("monitoring", false)
        set(v) = sp.edit().putBoolean("monitoring", v).apply()

    fun isIgnored(pkg: String): Boolean =
        sp.getStringSet("ignored", emptySet())!!.contains(pkg)

    fun setIgnored(pkg: String, ignored: Boolean) {
        val s = sp.getStringSet("ignored", emptySet())!!.toMutableSet()
        if (ignored) s.add(pkg) else s.remove(pkg)
        sp.edit().putStringSet("ignored", s).apply()
    }

    fun ignoredApps(): Set<String> =
        sp.getStringSet("ignored", emptySet())!!.toSet()

    fun lastVersionCode(pkg: String): Long = sp.getLong("ver_$pkg", -1L)
    fun setLastVersionCode(pkg: String, v: Long) {
        sp.edit().putLong("ver_$pkg", v).apply()
    }

    /** Consecutive failed changelog fetches for one exact app version. */
    fun fetchFailures(pkg: String, versionCode: Long): Int =
        sp.getInt("fail_${pkg}_$versionCode", 0)

    fun setFetchFailures(pkg: String, versionCode: Long, n: Int) {
        sp.edit().putInt("fail_${pkg}_$versionCode", n).apply()
    }

    fun lastChangelog(pkg: String): String? = sp.getString("log_$pkg", null)
    fun setLastChangelog(pkg: String, text: String) {
        sp.edit().putString("log_$pkg", text).apply()
    }

    /** Human-readable description of what the watcher last did (shown in the app). */
    var lastEvent: String
        get() = sp.getString("last_event", "Nothing yet — open any app, then check back here.")!!
        set(v) = sp.edit().putString("last_event", v).apply()

    /**
     * Timestamped ring buffer of watcher events (shown in the app's activity log).
     * This is the diagnostic trail: every fetch, popup, and failure is recorded
     * here so problems can be diagnosed from the device.
     */
    fun addEvent(msg: String) {
        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            .format(java.util.Date())
        val updated = appendLog(eventLog(), "$time — $msg", MAX_EVENTS)
        sp.edit().putString("event_log", updated).apply()
        lastEvent = "$time — $msg"
    }

    fun eventLog(): String = sp.getString("event_log", "") ?: ""

    fun clearLog() {
        sp.edit().remove("event_log").remove("last_event").apply()
    }

    companion object {
        private const val MAX_EVENTS = 40

        /** Pure helper (unit-testable): append an entry, keeping at most [max] lines. */
        fun appendLog(existing: String, entry: String, max: Int): String {
            val lines = existing.split("\n").filter { it.isNotBlank() }.toMutableList()
            lines.add(entry)
            while (lines.size > max) lines.removeAt(0)
            return lines.joinToString("\n")
        }
    }
}
