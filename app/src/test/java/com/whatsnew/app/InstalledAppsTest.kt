package com.whatsnew.app

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-logic tests for the installed-apps listing and the widget goal math. */
class InstalledAppsTest {

    private fun flags(vararg set: Int): Int =
        set.fold(0) { acc, f -> acc or f }

    @Test
    fun userApp_plainApp_isUserApp() {
        assertTrue(InstalledApps.isUserApp(flags()))
    }

    @Test
    fun userApp_systemApp_isNotUserApp() {
        assertFalse(InstalledApps.isUserApp(flags(ApplicationInfo.FLAG_SYSTEM)))
    }

    @Test
    fun userApp_updatedSystemApp_isUserApp() {
        assertTrue(
            InstalledApps.isUserApp(
                flags(
                    ApplicationInfo.FLAG_SYSTEM,
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                )
            )
        )
    }

    @Test
    fun userApp_otherFlags_doNotMatter() {
        assertTrue(InstalledApps.isUserApp(flags(ApplicationInfo.FLAG_DEBUGGABLE)))
        assertFalse(
            InstalledApps.isUserApp(
                flags(ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.FLAG_DEBUGGABLE)
            )
        )
    }

    @Test
    fun slotsLeft_underGoal() {
        assertEquals(23, InstalledApps.slotsLeft(127, 150))
    }

    @Test
    fun slotsLeft_overGoal_isZero() {
        assertEquals(0, InstalledApps.slotsLeft(160, 150))
    }

    @Test
    fun goalLine_underGoal() {
        assertEquals("23 slots left · goal 150", InstalledApps.goalLine(127, 150))
    }

    @Test
    fun goalLine_overGoal() {
        assertEquals("10 over your goal of 150", InstalledApps.goalLine(160, 150))
    }

    @Test
    fun goalLine_exactlyAtGoal() {
        assertEquals("0 slots left · goal 150", InstalledApps.goalLine(150, 150))
    }

    private fun app(pkg: String, name: String) =
        InstalledAppInfo(pkg, name, "1.0", 1024L, null)

    @Test
    fun filterByQuery_matchesNameCaseInsensitive() {
        val apps = listOf(app("com.spotify.music", "Spotify"), app("com.lyft", "Lyft"))
        assertEquals(listOf(apps[0]), InstalledApps.filterByQuery(apps, "spot"))
    }

    @Test
    fun filterByQuery_matchesPackageName() {
        val apps = listOf(app("com.spotify.music", "Spotify"), app("com.lyft", "Lyft"))
        assertEquals(listOf(apps[1]), InstalledApps.filterByQuery(apps, "lyft"))
    }

    @Test
    fun filterByQuery_blankReturnsAll() {
        val apps = listOf(app("a", "A"), app("b", "B"))
        assertEquals(apps, InstalledApps.filterByQuery(apps, "   "))
    }

    @Test
    fun filterByQuery_noMatchReturnsEmpty() {
        val apps = listOf(app("a", "A"))
        assertTrue(InstalledApps.filterByQuery(apps, "zzz").isEmpty())
    }

    @Test
    fun sortByName_isCaseInsensitive() {
        val apps = listOf(app("b", "banana"), app("a", "Apple"), app("c", "cherry"))
        assertEquals(
            listOf("Apple", "banana", "cherry"),
            InstalledApps.sortByName(apps).map { it.name }
        )
    }

    @Test
    fun formatSize_bytes() {
        assertEquals("512 B", InstalledApps.formatSize(512))
    }

    @Test
    fun formatSize_kilobytes() {
        assertEquals("850 KB", InstalledApps.formatSize(850 * 1024))
    }

    @Test
    fun formatSize_megabytes() {
        assertEquals("1.2 MB", InstalledApps.formatSize((1.2 * 1024 * 1024).toLong()))
    }
}
