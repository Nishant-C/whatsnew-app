package com.whatsnew.app

import android.Manifest
import android.app.AppOpsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsnew.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: IgnoredAppsAdapter

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.toolbar.title = getString(R.string.app_name)
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
        binding.toolbar.setNavigationOnClickListener { showMenu() }

        binding.howItWorks.text =
            "How it works:\n" +
                "1. Grant the two permissions below.\n" +
                "2. Turn on monitoring.\n" +
                "3. The first time you open each app, you'll see its current Play Store " +
                "\"What's new\" once — this also confirms the app is working.\n" +
                "4. After that, a popup appears only when the app updates AND its changelog changed.\n" +
                "5. Tap \"Ignore this app\" on any popup to never hear about that app again.\n" +
                "6. The ☰ menu (top left) opens the full activity log — check it if something looks wrong."

        adapter = IgnoredAppsAdapter(
            onRemove = { pkg ->
                prefs.setIgnored(pkg, false)
                refreshIgnored()
                Toast.makeText(this, "Removed from ignore list", Toast.LENGTH_SHORT).show()
            },
            labelFor = { pkg -> appLabel(pkg) }
        )
        binding.ignoredList.layoutManager = LinearLayoutManager(this)
        binding.ignoredList.adapter = adapter
        binding.ignoredList.isNestedScrollingEnabled = false

        binding.monitorSwitch.isChecked = prefs.monitoringEnabled
        binding.monitorSwitch.setOnCheckedChangeListener { _, on ->
            prefs.monitoringEnabled = on
            if (on) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                promptForPermissions()
            }
            syncService()
            refresh()
        }

        binding.usageBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.overlayBtn.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        binding.testPopupBtn.setOnClickListener {
            if (prefs.monitoringEnabled && hasUsageAccess() && hasOverlay()) {
                AppWatchService.testPopup(this)
            } else {
                Toast.makeText(
                    this,
                    "Turn on monitoring and grant both permissions first",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        binding.eventLog.movementMethod = ScrollingMovementMethod.getInstance()
        // The outer screen scrolls too — without this it steals the gestures and
        // the log box never scrolls.
        binding.eventLog.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_MOVE
            ) {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
        binding.copyLogBtn.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(
                ClipData.newPlainText("What's New activity log", prefs.eventLog())
            )
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        syncService()
        refresh()
    }

    private fun showMenu() {
        val popup = androidx.appcompat.widget.PopupMenu(this, binding.toolbar)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_logs -> {
                    startActivity(Intent(this, LogsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlay(): Boolean = Settings.canDrawOverlays(this)

    private fun promptForPermissions() {
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Please grant Usage access", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (!hasOverlay()) {
            Toast.makeText(this, "Please allow display over other apps", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun syncService() {
        if (prefs.monitoringEnabled && hasUsageAccess() && hasOverlay()) {
            AppWatchService.start(this)
        } else {
            AppWatchService.stop(this)
        }
    }

    private fun refresh() {
        binding.usageStatus.text = if (hasUsageAccess()) "Granted ✓" else "Not granted"
        binding.overlayStatus.text = if (hasOverlay()) "Granted ✓" else "Not granted"
        val running = prefs.monitoringEnabled && hasUsageAccess() && hasOverlay()
        binding.serviceStatus.text = if (running) "Status: watching ✓" else "Status: off"
        binding.eventLog.text = prefs.eventLog()
            .ifBlank { "Nothing yet — open any app, then check back here." }
        refreshIgnored()
    }

    private fun refreshIgnored() {
        val list = prefs.ignoredApps().sorted()
        adapter.submit(list)
        binding.ignoredEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun appLabel(pkg: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg
        }
    }
}
