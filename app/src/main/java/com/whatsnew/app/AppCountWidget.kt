package com.whatsnew.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews

/**
 * Home-screen widget: your live installed-app count against your goal.
 * Two sizes share this logic — the 2x2 "Clean Count" and the compact 2x1 —
 * each in a light and a dark variant chosen from the system theme.
 * The count refreshes whenever an app is installed, removed, or updated
 * (package broadcasts), when the goal changes in the app, and when the
 * system theme flips (configuration change).
 */
object AppCountWidget {

    fun isNightMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val night = isNightMode(context)
        updateFor(
            mgr, context, AppCountWidgetProvider::class.java,
            if (night) R.layout.widget_app_count_dark else R.layout.widget_app_count
        )
        updateFor(
            mgr, context, AppCountWidgetSmallProvider::class.java,
            if (night) R.layout.widget_app_count_small_dark
            else R.layout.widget_app_count_small
        )
    }

    private fun updateFor(
        mgr: AppWidgetManager,
        context: Context,
        provider: Class<*>,
        layoutRes: Int
    ) {
        val ids = mgr.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        mgr.updateAppWidget(ids, buildViews(context, layoutRes))
    }

    /** Pure RemoteViews construction: same view ids in both size layouts. */
    fun buildViews(context: Context, layoutRes: Int): RemoteViews {
        val count = InstalledApps.countUserApps(context.packageManager)
        val goal = Prefs(context).appCountGoal
        val views = RemoteViews(context.packageName, layoutRes)
        views.setTextViewText(R.id.widgetCount, count.toString())
        views.setTextViewText(R.id.widgetGoal, InstalledApps.goalLine(count, goal))
        views.setProgressBar(R.id.widgetProgress, goal, minOf(count, goal), false)
        val intent = Intent(context, InstalledAppsActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pending)
        return views
    }
}

/** Shared provider behavior: refresh on widget update and on package changes. */
abstract class AppCountWidgetProviderBase : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        AppCountWidget.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_CONFIGURATION_CHANGED -> AppCountWidget.updateAll(context)
            else -> super.onReceive(context, intent)
        }
    }
}

/** The 2x2 Clean Count widget. */
class AppCountWidgetProvider : AppCountWidgetProviderBase()

/** The compact 2x1 Clean Count widget. */
class AppCountWidgetSmallProvider : AppCountWidgetProviderBase()
