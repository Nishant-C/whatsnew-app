package com.whatsnew.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.whatsnew.app.databinding.OverlayChangelogBinding

/**
 * Shows the changelog as an overlay card on top of the opened app.
 *
 * Fallback chain: system overlay -> transparent popup activity -> notification.
 * Every failure is reported through [onEvent] so the in-app activity log shows
 * exactly what went wrong on the device.
 */
object OverlayManager {
    private var currentView: View? = null

    fun show(
        context: Context,
        pkg: String,
        appName: String,
        versionName: String,
        changelog: String,
        onIgnore: () -> Unit,
        onEvent: (String) -> Unit = {}
    ) {
        Handler(Looper.getMainLooper()).post {
            if (currentView != null) return@post
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            try {
                // Inflate with the app's Material theme: a bare Service context has
                // no theme attributes, which crashes MaterialCardView inflation.
                val themed = ContextThemeWrapper(context, R.style.Theme_WhatsNew)
                val binding =
                    OverlayChangelogBinding.inflate(LayoutInflater.from(themed))
                binding.title.text =
                    if (versionName.isNotBlank()) "$appName updated • $versionName"
                    else "$appName updated"
                binding.changelog.text = changelog
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                )
                fun close() {
                    try {
                        wm.removeView(currentView)
                    } catch (_: Exception) {
                    }
                    currentView = null
                }
                binding.dismissBtn.setOnClickListener { close() }
                binding.ignoreBtn.setOnClickListener { onIgnore(); close() }
                binding.scrim.setOnClickListener { close() }
                // Only mark the view as live after addView succeeds: if it
                // throws, a stale currentView would block every later popup.
                wm.addView(binding.root, params)
                currentView = binding.root
                onEvent("Overlay shown for $appName.")
            } catch (e: Exception) {
                currentView = null
                // Record the real reason (e.g. "SecurityException: Permission denied
                // for window type 2038") instead of failing silently.
                onEvent(
                    "Overlay failed for $appName " +
                        "(${e.javaClass.simpleName}: ${e.message}) — trying popup screen."
                )
                val popped = PopupActivity.show(context, pkg, appName, versionName, changelog)
                if (popped) {
                    onEvent("Popup screen shown for $appName (overlay unavailable).")
                } else {
                    onEvent("Popup screen failed for $appName — using notification.")
                    notifyFallback(context, appName, changelog)
                }
            }
        }
    }

    private fun notifyFallback(context: Context, appName: String, changelog: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("changelog", "Changelogs", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, "changelog")
            .setContentTitle("$appName updated — what's new")
            .setContentText(changelog.take(200))
            .setStyle(NotificationCompat.BigTextStyle().bigText(changelog))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(appName.hashCode(), n)
        } catch (_: Exception) {
        }
    }
}
