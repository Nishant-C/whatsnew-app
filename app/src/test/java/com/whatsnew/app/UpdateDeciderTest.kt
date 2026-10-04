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

    // decideOnOpen: the two-phase open (silent fetch, then instant popup)

    @Test
    fun `version change fetches in background without popping up`() {
        assertEquals(
            OpenAction.FetchInBackground,
            UpdateDecider.decideOnOpen(
                ignored = false, storedVersion = 499L, currentVersion = 500L,
                storedChangelog = "Old news.", shownChangelog = "Old news."
            )
        )
    }

    @Test
    fun `first sighting fetches in background without popping up`() {
        assertEquals(
            OpenAction.FetchInBackground,
            UpdateDecider.decideOnOpen(
                ignored = false, storedVersion = -1L, currentVersion = 500L,
                storedChangelog = null, shownChangelog = null
            )
        )
    }

    @Test
    fun `next open after a silent fetch shows the stored changelog instantly`() {
        assertEquals(
            OpenAction.ShowStored("New stickers!"),
            UpdateDecider.decideOnOpen(
                ignored = false, storedVersion = 500L, currentVersion = 500L,
                storedChangelog = "New stickers!", shownChangelog = "Bug fixes."
            )
        )
    }

    @Test
    fun `already-shown changelog does nothing`() {
        assertEquals(
            OpenAction.Nothing,
            UpdateDecider.decideOnOpen(
                ignored = false, storedVersion = 500L, currentVersion = 500L,
                storedChangelog = "Bug fixes.", shownChangelog = "Bug fixes."
            )
        )
    }

    @Test
    fun `unchanged changelog after update does nothing on next open`() {
        // Update fetched, but the text matches what was already shown.
        assertEquals(
            OpenAction.Nothing,
            UpdateDecider.decideOnOpen(
                ignored = false, storedVersion = 500L, currentVersion = 500L,
                storedChangelog = "Bug fixes.", shownChangelog = "Bug fixes."
            )
        )
    }

    @Test
    fun `ignored app never fetches nor shows`() {
        assertEquals(
            OpenAction.Nothing,
            UpdateDecider.decideOnOpen(
                ignored = true, storedVersion = 499L, currentVersion = 500L,
                storedChangelog = "New stickers!", shownChangelog = null
            )
        )
    }
}
