# What's New

An Android app that shows you an app's Play Store "What's new" changelog as a popup
when you open an app that just updated — but only if the changelog text actually changed.

## How it works

1. A foreground service polls `UsageStatsManager` every 2 seconds to detect which app
   is in the foreground (screen must be on).
2. For each newly opened app it compares the installed `versionCode` against the last
   one it recorded. The first time an app is seen, its current changelog is fetched
   and shown once (this also confirms the watcher is working); afterwards a popup
   appears only on real updates as described below.
3. If the version increased, it loads the app's Play Store page in a hidden WebView
   (the Play Store has no public changelog API, and the changelog isn't in the raw
   page HTML — it's rendered by the site's scripts) and extracts the "What's new"
   section. If the fetch fails, it retries the next time the app is opened (up to
   3 tries), then that version is skipped so apps with no Play Store listing don't
   refetch forever.
4. If the extracted text differs from the last shown text, an overlay card pops up
   over the opened app with the changelog. Buttons: **Got it** (dismiss) and
   **Ignore this app** (adds it to the ignore list, managed in the main screen).
5. If the system overlay is rejected at popup time, the app tries a transparent
   popup screen instead, and only then falls back to a notification. Any failure
   (including the exact system error) is written to the activity log.
6. The main screen shows an **Activity log** with timestamped entries describing
   what the watcher did (which app it saw, fetch results, popup attempts and any
   errors), a **Copy log** button to share diagnostics, plus a **Test popup now**
   button that shows a sample popup so you can verify the popup path works
   without waiting for an update.

## Permissions (all granted by you in Settings — no root)

- **Usage access** (`PACKAGE_USAGE_STATS`) — detect which app you opened.
- **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — show the popup card.
- **Notifications** (`POST_NOTIFICATIONS`, Android 13+) — foreground-service notice
  and the fallback path.
- **Internet** — fetch Play Store pages.

## Build

Android Studio: open the `WhatsNew` folder and run the `app` configuration.

Command line (needs JDK 17 and the Android SDK with platform 34 + build-tools 34):

```bash
export ANDROID_SDK_ROOT=/path/to/android-sdk
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Install

Copy `app-debug.apk` to your phone, open it, and allow "install unknown apps" when
prompted. Then open **What's New**, grant the two permissions, and flip on
"Watch my apps".

## Limitations (honest ones)

- Changelog extraction depends on the Play Store page layout. If Google redesigns
  the page, the extractor in `ChangelogFetcher.kt` may need updating.
- Fetching takes ~5–10 seconds (hidden browser render), so the popup appears a few
  seconds after you open the updated app.
- The service pauses while the screen is off to save battery.
- This is a debug build for sideloading, not a Play Store release.
