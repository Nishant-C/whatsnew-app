package com.whatsnew.app

/** Extracts the "What's new" changelog from rendered Play Store page text. */
object ChangelogParser {

    /** Grab the text between the "What's new" heading and the next section heading. */
    fun parse(pageText: String): String? {
        val heads = listOf("What's new", "What’s new")
        var start = -1
        var used = ""
        for (h in heads) {
            val i = pageText.indexOf(h)
            if (i >= 0 && (start == -1 || i < start)) {
                start = i
                used = h
            }
        }
        if (start < 0) return null
        var body = pageText.substring(start + used.length)
        val followers = listOf(
            "Ratings and reviews", "About this app", "Data safety",
            "Developer contact", "App support", "Flag as inappropriate",
            "More by", "Similar apps", "You might also like",
            "Events and offers", "In-app purchases", "Editors' Choice"
        )
        var end = body.length
        for (h in followers) {
            val i = body.indexOf(h)
            if (i > 0 && i < end) end = i
        }
        body = body.substring(0, end).trim()
        if (body.length > 3000) body = body.substring(0, 3000).trim()
        return body.ifBlank { null }
    }
}
