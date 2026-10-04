package com.whatsnew.app

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.whatsnew.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(PrefsBackup.toJson(prefs.exportAll()).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "Backup saved — keep the file safe", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                val json = contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: return@registerForActivityResult
                prefs.importAll(PrefsBackup.fromJson(json))
                Toast.makeText(
                    this,
                    "Data restored — toggle monitoring off and on to apply",
                    Toast.LENGTH_LONG
                ).show()
                refresh()
            } catch (e: Exception) {
                Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

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
                "3. The first time you open each app, its current Play Store " +
                "\"What's new\" is fetched quietly in the background — you'll see it " +
                "the next time you open that app. This also confirms the app is working.\n" +
                "4. When an app updates, its new changelog is fetched silently and " +
                "shown instantly the next time you open the app — no waiting. " +
                "A popup appears only when the changelog text actually changed.\n" +
                "5. Tap \"Ignore this app\" on any popup to never hear about that app again.\n" +
                "6. The ☰ menu (top left) holds the activity log, the ignored-apps list, " +
                "and the latest update info fetched for each app."

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
                R.id.menu_ignored -> {
                    startActivity(Intent(this, IgnoredAppsActivity::class.java))
                    true
                }
                R.id.menu_updates -> {
                    startActivity(Intent(this, UpdatesActivity::class.java))
                    true
                }
                R.id.menu_backup -> {
                    backupLauncher.launch("whatsnew-backup.json")
                    true
                }
                R.id.menu_restore -> {
                    restoreLauncher.launch(arrayOf("application/json"))
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
    }
}
