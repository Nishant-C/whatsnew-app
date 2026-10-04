package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class UpdateMetaTest {

    @Test
    fun `metaLine shows version and formatted fetch time`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        // 2026-09-21 19:05:00 PDT
        val line = UpdateMeta.metaLine("2.1", 1790042700000L)
        assertTrue(line.startsWith("v2.1 • fetched "))
        assertTrue(line.contains("Sep 21"))
    }

    @Test
    fun `metaLine falls back when version or time unknown`() {
        assertEquals("v? • fetched unknown time", UpdateMeta.metaLine(null, 0L))
        assertEquals("v? • fetched unknown time", UpdateMeta.metaLine("  ", 0L))
    }
}
