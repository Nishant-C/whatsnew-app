package com.whatsnew.app

/**
 * Pure decision logic for which navigations the changelog WebView may perform.
 *
 * The Play Store page tries to deep-link into the Play Store app via non-web
 * schemes (intent://, market://). A WebView cannot handle those, so they must be
 * swallowed — otherwise they surface as ERR_UNKNOWN_URL_SCHEME on the main frame
 * and kill the fetch.
 */
object SchemePolicy {
    /** True when the WebView should load the URL itself. */
    fun allowLoad(scheme: String?): Boolean = scheme == "http" || scheme == "https"
}
