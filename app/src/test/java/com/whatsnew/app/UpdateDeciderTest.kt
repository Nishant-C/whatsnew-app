package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Mirrors the user's scenarios:
 *  - open app, no update -> nothing
 *  - open app, updated but changelog unchanged -> nothing
 *  - open app, updated and changelog changed -> popup
 *  - ignored app -> never anything
 */
class UpdateDeciderTest {

    @Test
    fun `first sighting of an app fetches its current changelog once`() {
        assertTrue(UpdateDecider.shouldFetch(ignored = false, storedVersion = -1L, currentVersion = 500L))
    }

    @Test
    fun `ignored app never fetches, not even on first sighting`() {
        assertFalse(UpdateDecider.shouldFetch(ignored = true, storedVersion = -1L, currentVersion = 500L))
    }

    @Test
    fun `same version opened again does nothing`() {
        assertFalse(UpdateDecider.shouldFetch(ignored = false, storedVersion = 500L, currentVersion = 500L))
    }

    @Test
    fun `version increase triggers a fetch`() {
        assertTrue(UpdateDecider.shouldFetch(ignored = false, storedVersion = 499L, currentVersion = 500L))
    }

    @Test
    fun `ignored app never fetches even after update`() {
        assertFalse(UpdateDecider.shouldFetch(ignored = true, storedVersion = 499L, currentVersion = 500L))
    }

    @Test
    fun `blank changelog never pops up`() {
        assertFalse(UpdateDecider.shouldShowPopup("   ", "something old"))
        assertFalse(UpdateDecider.shouldShowPopup(null, "something old"))
        assertFalse(UpdateDecider.shouldShowPopup("", null))
    }

    @Test
    fun `unchanged changelog does not pop up again`() {
        assertFalse(UpdateDecider.shouldShowPopup("Bug fixes.", "Bug fixes."))
        assertFalse(UpdateDecider.shouldShowPopup("  Bug fixes.\n", "Bug fixes."))
    }

    @Test
    fun `changed changelog pops up`() {
        assertTrue(UpdateDecider.shouldShowPopup("New stickers!", "Bug fixes."))
    }

    @Test
    fun `first ever changelog pops up`() {
        assertTrue(UpdateDecider.shouldShowPopup("New stickers!", null))
    }
}
