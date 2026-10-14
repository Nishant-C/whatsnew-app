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

    /**
     * Records the latest fetched changelog for [pkg], stamping when it was
     * fetched. Called on every successful fetch (not only when the text
     * changed) so the "Latest updates" screen always shows fresh info.
     */
    fun setLastChangelog(pkg: String, text: String) {
        val pkgs = sp.getStringSet("changelog_pkgs", emptySet())!!.toMutableSet()
        pkgs.add(pkg)
        sp.edit()
            .putString("log_$pkg", text)
            .putLong("logtime_$pkg", System.currentTimeMillis())
            .putStringSet("changelog_pkgs", pkgs)
            .apply()
    }

    /** Packages with a fetched changelog, newest fetch first. */
    fun changelogPackages(): List<String> =
        sp.getStringSet("changelog_pkgs", emptySet())!!
            .sortedByDescending { changelogFetchedAt(it) }

    fun changelogFetchedAt(pkg: String): Long = sp.getLong("logtime_$pkg", 0L)

    fun lastVersionName(pkg: String): String? = sp.getString("vname_$pkg", null)
    fun setLastVersionName(pkg: String, name: String) {
        sp.edit().putString("vname_$pkg", name).apply()
    }

    /**
     * The changelog text the popup last displayed for [pkg]. Compared against
     * the latest fetched text: when they differ, the next app open shows the
     * popup instantly from stored data — no fetch, no lag.
     */
    fun shownChangelog(pkg: String): String? = sp.getString("shown_$pkg", null)
    fun setShownChangelog(pkg: String, text: String) {
        sp.edit().putString("shown_$pkg", text).apply()
    }

    /**
     * One-time migration: changelogs fetched before the shown-tracking
     * existed count as already shown, so updating the app doesn't pop a
     * dialog for every previously-fetched app on its next open.
     */
    fun migrateShownChangelogs() {
        if (sp.getBoolean("shown_migrated", false)) return
        val e = sp.edit()
        for (pkg in sp.getStringSet("changelog_pkgs", emptySet())!!) {
            sp.getString("log_$pkg", null)?.let { e.putString("shown_$pkg", it) }
        }
        e.putBoolean("shown_migrated", true).apply()
    }

    /** Target for the "Installed apps" screen and the home-screen widget. */
    var appCountGoal: Int
        get() = sp.getInt("app_count_goal", 150)
        set(v) = sp.edit().putInt("app_count_goal", v.coerceAtLeast(1)).apply()

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

    /** Dumps every stored entry (key -> typed value) for backup. */
    fun exportAll(): Map<String, Any?> = sp.all.toMap()

    /** Restores entries previously dumped by [exportAll]; replaces everything. */
    fun importAll(entries: Map<String, Any?>) {
        val e = sp.edit().clear()
        for ((k, v) in entries) {
            when (v) {
                is String -> e.putString(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Float -> e.putFloat(k, v)
                is Boolean -> e.putBoolean(k, v)
                is Set<*> -> e.putStringSet(k, v.filterIsInstance<String>().toSet())
                null -> e.remove(k)
            }
        }
        e.apply()
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
