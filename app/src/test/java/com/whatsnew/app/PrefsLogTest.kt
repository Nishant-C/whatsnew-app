package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class PrefsLogTest {

    @Test
    fun `appendLog keeps entries in order`() {
        val log = Prefs.appendLog("", "10:00 AM — first", 40)
        val log2 = Prefs.appendLog(log, "10:01 AM — second", 40)
        assertEquals("10:00 AM — first\n10:01 AM — second", log2)
    }

    @Test
    fun `appendLog trims oldest entries beyond max`() {
        var log = ""
        for (i in 1..45) log = Prefs.appendLog(log, "entry $i", 40)
        val lines = log.split("\n")
        assertEquals(40, lines.size)
        assertEquals("entry 6", lines.first())
        assertEquals("entry 45", lines.last())
    }

    @Test
    fun `appendLog ignores blank lines in existing log`() {
        val log = Prefs.appendLog("\n\n", "only entry", 40)
        assertEquals("only entry", log)
    }
}
