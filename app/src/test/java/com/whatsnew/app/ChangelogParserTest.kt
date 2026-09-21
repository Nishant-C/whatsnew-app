package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class ChangelogParserTest {

    private fun pageWith(vararg sections: String): String =
        sections.joinToString("\n")

    @Test
    fun `extracts changelog between What's new and next heading`() {
        val page = pageWith(
            "Instagram",
            "About this app",
            "Share your moments.",
            "Data safety",
            "Safety practices...",
            "What's new",
            "• Bug fixes and performance improvements.",
            "• New stickers in Stories.",
            "Ratings and reviews",
            "4.0 ★"
        )
        assertEquals(
            "• Bug fixes and performance improvements.\n• New stickers in Stories.",
            ChangelogParser.parse(page)
        )
    }

    @Test
    fun `handles curly apostrophe heading`() {
        val page = pageWith("What’s new", "Fixed crashes.", "Developer contact")
        assertEquals("Fixed crashes.", ChangelogParser.parse(page))
    }

    @Test
    fun `returns null when there is no What's new section`() {
        assertNull(ChangelogParser.parse(pageWith("About this app", "Hello world")))
    }

    @Test
    fun `returns null when What's new body is empty`() {
        assertNull(ChangelogParser.parse(pageWith("What's new", "   ", "Developer contact")))
    }

    @Test
    fun `works when What's new is the last section on the page`() {
        val page = pageWith("Ratings and reviews", "4.0", "What's new", "Just bug fixes.")
        assertEquals("Just bug fixes.", ChangelogParser.parse(page))
    }

    @Test
    fun `truncates very long changelogs`() {
        val long = "word ".repeat(2000)
        val page = pageWith("What's new", long, "Ratings and reviews")
        val result = ChangelogParser.parse(page)
        assertNotNull(result)
        assertTrue(result!!.length <= 3000)
    }

    @Test
    fun `does not confuse description text mentioning updates`() {
        // "what's new" in lowercase inside the description must not match the heading.
        val page = pageWith(
            "About this app",
            "See what's new with your friends every day.",
            "What's new",
            "Real changelog here.",
            "Data safety"
        )
        assertEquals("Real changelog here.", ChangelogParser.parse(page))
    }

    @Test
    fun `real Redfin page stops before App support section`() {
        // Mirrors the current Play Store layout: "What's new" is followed by
        // "Flag as inappropriate" / "App support" (with phone numbers and addresses).
        val page = pageWith(
            "Redfin: Buy, Sell & Rent Homes",
            "Data safety",
            "Safety starts with understanding how developers collect and share your data.",
            "Ratings and reviews",
            "Phone, Chromebook, Tablet, 4.7, 163K reviews",
            "What’s new",
            "Thanks for using Redfin for your home search! This version includes several bug fixes and performance improvements.",
            "We're always looking for ways to improve the real estate experience for consumers.",
            "Flag as inappropriate",
            "App support",
            "Website",
            "Phone number, +18779733346",
            "Support email, customer-service@redfin.com",
            "1099 Stewart St Ste 600",
            "Seattle, WA 98101",
            "Similar apps",
            "OfferUp Buy & Sell Marketplace"
        )
        val result = ChangelogParser.parse(page)
        assertEquals(
            "Thanks for using Redfin for your home search! This version includes several bug fixes " +
                "and performance improvements.\n" +
                "We're always looking for ways to improve the real estate experience for consumers.",
            result
        )
        assertFalse(result!!.contains("App support"))
        assertFalse(result.contains("+18779733346"))
    }
}
