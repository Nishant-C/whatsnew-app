package com.whatsnew.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONTokener

/**
 * Fetches an app's Play Store "What's new" section.
 *
 * The Play Store has no public changelog API and the changelog is not present in the
 * initial page HTML (it is loaded by the site's own scripts), so we render the page in
 * a hidden WebView exactly like a browser would, then extract the rendered text.
 */
class ChangelogFetcher(private val context: Context) {

    /**
     * [onEvent] receives human-readable diagnostics (page failed, timed out, no
     * section found) so the in-app activity log shows why a fetch produced nothing.
     */
    fun fetch(
        packageName: String,
        appName: String,
        onEvent: (String) -> Unit,
        callback: (String?) -> Unit
    ) {
        val main = Handler(Looper.getMainLooper())
        main.post {
            var finished = false
            var webView: WebView? = null
            lateinit var timeout: Runnable

            fun finish(result: String?) {
                if (finished) return
                finished = true
                main.removeCallbacks(timeout)
                try {
                    webView?.stopLoading()
                    webView?.destroy()
                } catch (_: Exception) {
                }
                webView = null
                callback(result)
            }

            fun extract(attempt: Int) {
                val wv = webView ?: return
                if (finished) return
                wv.evaluateJavascript(
                    "(function(){try{window.scrollTo(0,document.body.scrollHeight);}catch(e){}" +
                        "return document.documentElement.innerText||'';})()"
                ) { value ->
                    if (finished) return@evaluateJavascript
                    val pageText = decodeJsString(value)
                    val changelog = ChangelogParser.parse(pageText)
                    if (changelog != null || attempt >= 2) {
                        if (changelog == null) {
                            onEvent(
                                "No \"What's new\" section found on the Play Store page " +
                                    "for $appName (read ${pageText.length} chars of page text)."
                            )
                        }
                        finish(changelog)
                    } else main.postDelayed({ extract(attempt + 1) }, 3000)
                }
            }

            timeout = Runnable {
                onEvent("Timed out reading the Play Store page for $appName.")
                finish(null)
            }

            val wv = WebView(context.applicationContext)
            webView = wv
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.loadsImagesAutomatically = false
            wv.settings.blockNetworkImage = true
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    main.postDelayed({ extract(0) }, 5000)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    // The Play Store page tries to deep-link into the Play Store app
                    // (intent://, market://). The WebView can't handle those schemes:
                    // swallow them here so they never surface as
                    // ERR_UNKNOWN_URL_SCHEME and kill the fetch.
                    return !SchemePolicy.allowLoad(request.url.scheme)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        if (error.errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME) {
                            // A swallowed app deep link (net::ERR_UNKNOWN_URL_SCHEME);
                            // the page itself is fine.
                            return
                        }
                        onEvent(
                            "Play Store page failed to load for $appName " +
                                "(${error.errorCode}: ${error.description})."
                        )
                        finish(null)
                    }
                }
            }
            main.postDelayed(timeout, 40_000)
            wv.loadUrl("https://play.google.com/store/apps/details?id=$packageName&hl=en&gl=US")
        }
    }

    /** evaluateJavascript returns a JSON-encoded string; decode it. */
    private fun decodeJsString(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return try {
            (JSONTokener(value).nextValue() as? String) ?: value
        } catch (_: Exception) {
            value
        }
    }
}
