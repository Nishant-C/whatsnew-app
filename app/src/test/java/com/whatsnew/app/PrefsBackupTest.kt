package com.whatsnew.app

import org.junit.Assert.*
import org.junit.Test

class PrefsBackupTest {

    @Test
    fun `round trip preserves every supported type`() {
        val entries: Map<String, Any?> = mapOf(
            "monitoring" to true,
            "ver_com.example" to 500L,
            "fail_com.example_500" to 2,
            "ratio" to 1.5f,
            "log_com.example" to "Line 1\nLine 2 \"quoted\" • bullet",
            "ignored" to setOf("com.a", "com.b"),
            "gone" to null
        )
        val restored = PrefsBackup.fromJson(PrefsBackup.toJson(entries))
        assertEquals(true, restored["monitoring"])
        assertEquals(500L, restored["ver_com.example"])
        assertEquals(2, restored["fail_com.example_500"])
        assertEquals(1.5f, restored["ratio"])
        assertEquals("Line 1\nLine 2 \"quoted\" • bullet", restored["log_com.example"])
        assertEquals(setOf("com.a", "com.b"), restored["ignored"])
        assertTrue(restored.containsKey("gone"))
        assertNull(restored["gone"])
    }

    @Test
    fun `empty map round trips to empty map`() {
        assertTrue(PrefsBackup.fromJson(PrefsBackup.toJson(emptyMap())).isEmpty())
    }

    @Test
    fun `toJson output is a JSON object`() {
        val json = PrefsBackup.toJson(mapOf("monitoring" to true))
        assertTrue(json.startsWith("{"))
        assertTrue(json.contains("\"monitoring\""))
    }
}
