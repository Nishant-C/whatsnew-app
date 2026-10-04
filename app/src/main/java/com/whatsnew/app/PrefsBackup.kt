package com.whatsnew.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup/restore for the app's SharedPreferences.
 *
 * Pure functions over Map<String, Any?> so they are unit-testable on the JVM;
 * the Android glue (Prefs.exportAll/importAll) just hands over sp.all.
 * Format: {"<key>": {"t": "<type>", "v": <value>}} with types
 * s/i/l/f/b/ss (string, int, long, float, boolean, string-set).
 */
object PrefsBackup {

    fun toJson(entries: Map<String, Any?>): String {
        val root = JSONObject()
        for ((key, value) in entries) {
            val entry = JSONObject()
            when (value) {
                is String -> {
                    entry.put("t", "s")
                    entry.put("v", value)
                }
                is Int -> {
                    entry.put("t", "i")
                    entry.put("v", value)
                }
                is Long -> {
                    entry.put("t", "l")
                    entry.put("v", value)
                }
                is Float -> {
                    entry.put("t", "f")
                    entry.put("v", value.toDouble())
                }
                is Boolean -> {
                    entry.put("t", "b")
                    entry.put("v", value)
                }
                is Set<*> -> {
                    entry.put("t", "ss")
                    entry.put("v", JSONArray(value.filterIsInstance<String>()))
                }
                null -> entry.put("t", "n")
                else -> continue // unknown type: skip rather than corrupt
            }
            root.put(key, entry)
        }
        return root.toString()
    }

    fun fromJson(json: String): Map<String, Any?> {
        val root = JSONObject(json)
        val out = mutableMapOf<String, Any?>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.getJSONObject(key)
            out[key] = when (entry.optString("t", "n")) {
                "s" -> entry.getString("v")
                "i" -> entry.getInt("v")
                "l" -> entry.getLong("v")
                "f" -> entry.getDouble("v").toFloat()
                "b" -> entry.getBoolean("v")
                "ss" -> entry.getJSONArray("v").let { arr ->
                    (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
                }
                else -> null
            }
        }
        return out
    }
}
