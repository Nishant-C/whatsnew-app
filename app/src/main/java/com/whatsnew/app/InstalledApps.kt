package com.whatsnew.app

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.io.File

/** One installed app, as shown on the "Installed apps" screen. */
data class InstalledAppInfo(
    val pkg: String,
    val name: String,
    val versionName: String?,
    val sizeBytes: Long,
    val icon: Drawable?
)

/**
 * Installed-app listing, shared by the "Installed apps" screen and the
 * home-screen widget. Only user-installed apps are listed: system apps
 * cannot be uninstalled, so counting them would make the "reduce your
 * apps" number un-actionable — and updated system apps can only have
 * their updates removed, not the app itself.
 */
object InstalledApps {

    /**
     * Whether an app with these [ApplicationInfo] flags counts as a
     * user-installed app: strictly apps without the system flag. System apps
     * cannot be uninstalled (at most disabled), and updated system apps can
     * only have their updates removed — not the app itself — so neither
     * belongs in an "apps you can uninstall" list.
     * Takes raw flags so it stays unit-testable without Android classes.
     */
    fun isUserApp(flags: Int): Boolean =
        (flags and ApplicationInfo.FLAG_SYSTEM) == 0

    fun filterUserApps(infos: List<ApplicationInfo>): List<ApplicationInfo> =
        infos.filter { isUserApp(it.flags) }

    /** Pure helper (unit-testable): apps left to remove before hitting [goal]. */
    fun slotsLeft(count: Int, goal: Int): Int = maxOf(0, goal - count)

    /** Pure helper: the one-line goal status shown in the widget and the header. */
    fun goalLine(count: Int, goal: Int): String =
        if (count <= goal) "${slotsLeft(count, goal)} slots left · goal $goal"
        else "${count - goal} over your goal of $goal"

    fun sortByName(apps: List<InstalledAppInfo>): List<InstalledAppInfo> =
        apps.sortedBy { it.name.lowercase() }

    /** Pure helper: matches app name or package name, case-insensitive. */
    fun filterByQuery(apps: List<InstalledAppInfo>, query: String): List<InstalledAppInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps
        return apps.filter { it.name.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
    }

    /** Pure helper: 850 KB / 1.2 MB style sizes. */
    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }

    /** Full listing (icons included): call off the main thread, it is slow. */
    fun load(pm: PackageManager): List<InstalledAppInfo> {
        val infos = filterUserApps(pm.getInstalledApplications(PackageManager.GET_META_DATA))
        val out = ArrayList<InstalledAppInfo>(infos.size)
        for (ai in infos) {
            val name = try {
                pm.getApplicationLabel(ai).toString()
            } catch (_: Exception) {
                ai.packageName
            }
            val version = try {
                pm.getPackageInfo(ai.packageName, 0)?.versionName
            } catch (_: Exception) {
                null
            }
            val size = try {
                File(ai.sourceDir).length()
            } catch (_: Exception) {
                0L
            }
            val icon = try {
                pm.getApplicationIcon(ai)
            } catch (_: Exception) {
                null
            }
            out.add(InstalledAppInfo(ai.packageName, name, version, size, icon))
        }
        return sortByName(out)
    }

    /** Fast count for the widget: no icons, no sizes. */
    fun countUserApps(pm: PackageManager): Int =
        filterUserApps(pm.getInstalledApplications(0)).size
}
