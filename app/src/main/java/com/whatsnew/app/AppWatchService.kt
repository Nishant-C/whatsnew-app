package com.whatsnew.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Foreground service that polls UsageStatsManager to detect foreground-app changes.
 * When an app's versionCode increased since we last saw it, it fetches the Play Store
 * "What's new" section; a popup is shown only if the changelog text itself changed.
 */
class AppWatchService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var handler: Handler
    private var lastForeground: String? = null
    private val skipPkgs = setOf("android", "com.android.systemui")
    private var consecutivePollFailures = 0
    private var lastPollFailureLogAt = 0L

    private val poll = object : Runnable {
        override fun run() {
            try {
                checkForeground()
                consecutivePollFailures = 0
            } catch (e: Exception) {
                consecutivePollFailures++
                val now = System.currentTimeMillis()
                // Rate-limited: a persistent fault must not flood the log.
                if (PollRateLimit.shouldLogFailure(now, lastPollFailureLogAt)) {
                    lastPollFailureLogAt = now
                    event(
                        "App watcher hit an error ($consecutivePollFailures in a row: " +
                            "${e.javaClass.simpleName}) — still watching."
                    )
                }
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        handler = Handler(Looper.getMainLooper())
        startForeground(NOTIF_ID, buildNotification())
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TEST_POPUP) {
            OverlayManager.show(
                this, packageName, "Test App", "1.0",
                "• This is a test popup.\n" +
                    "• If you can see this card, the overlay works.\n" +
                    "• Tap Got it to dismiss.",
                onIgnore = { event("Test popup: Ignore tapped.") },
                onEvent = { event(it) }
            )
            event("Test popup requested.")
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        super.onDestroy()
    }

    private fun checkForeground() {
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!power.isInteractive) return // save battery while screen is off

        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 10_000, now)
        val current = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return
        if (current == lastForeground) return
        lastForeground = current

        if (current == packageName || current in skipPkgs) return
        if (prefs.isIgnored(current)) return
        handleAppOpened(current)
    }

    private fun handleAppOpened(pkg: String) {
        Thread {
            try {
                val pi = packageManager.getPackageInfo(pkg, 0)
                val versionCode = pi.longVersionCode
                val stored = prefs.lastVersionCode(pkg)
                val appName = appNameOf(pkg)
                if (!UpdateDecider.shouldFetch(prefs.isIgnored(pkg), stored, versionCode)) {
                    event("Saw $appName — no update, nothing to do.")
                    return@Thread
                }
                val versionName = pi.versionName ?: ""
                event("Update found for $appName — fetching changelog…")

                ChangelogFetcher(this).fetch(pkg, appName, { event(it) }) { changelog ->
                    if (changelog.isNullOrBlank()) {
                        // Don't mark the version as seen: retry next time the app is
                        // opened, and give up after a few consecutive failures so
                        // apps with no Play Store listing don't refetch forever.
                        val fails = prefs.fetchFailures(pkg, versionCode) + 1
                        prefs.setFetchFailures(pkg, versionCode, fails)
                        when (FetchRetry.onFailure(fails - 1)) {
                            FetchRetry.Outcome.RETRY ->
                                event(
                                    "Couldn't read the changelog for $appName " +
                                        "(try $fails/${FetchRetry.MAX_ATTEMPTS}) — " +
                                        "will retry next time you open it."
                                )
                            FetchRetry.Outcome.GIVE_UP -> {
                                prefs.setLastVersionCode(pkg, versionCode)
                                event(
                                    "Couldn't read the changelog for $appName " +
                                        "after $fails tries — skipping this version."
                                )
                            }
                        }
                        return@fetch
                    }
                    prefs.setFetchFailures(pkg, versionCode, 0)
                    prefs.setLastVersionCode(pkg, versionCode)
                    if (!UpdateDecider.shouldShowPopup(changelog, prefs.lastChangelog(pkg))) {
                        event("Changelog for $appName unchanged — no popup.")
                        return@fetch
                    }
                    prefs.setLastChangelog(pkg, changelog)
                    event("Showing changelog for $appName.")
                    OverlayManager.show(
                        this, pkg, appName, versionName, changelog,
                        onIgnore = {
                            prefs.setIgnored(pkg, true)
                            event("$appName added to ignore list.")
                        },
                        onEvent = { event(it) }
                    )
                }
            } catch (e: Exception) {
                // Now recorded in the log instead of swallowed: with
                // QUERY_ALL_PACKAGES this should rarely happen.
                event("Couldn't inspect $pkg (${e.javaClass.simpleName}) — will retry next time.")
            }
        }.start()
    }

    private fun appNameOf(pkg: String): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun event(msg: String) {
        prefs.addEvent(msg)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "App watching", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("What's New is watching")
            .setContentText("You'll see changelogs when your apps update.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val POLL_MS = 2000L
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "watch"
        const val ACTION_TEST_POPUP = "com.whatsnew.app.TEST_POPUP"

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, AppWatchService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AppWatchService::class.java))
        }

        fun testPopup(ctx: Context) {
            ctx.startForegroundService(
                Intent(ctx, AppWatchService::class.java).setAction(ACTION_TEST_POPUP)
            )
        }
    }
}
