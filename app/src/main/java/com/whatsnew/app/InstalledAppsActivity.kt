package com.whatsnew.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsnew.app.databinding.ActivityInstalledBinding

/**
 * Every user-installed app, with a one-tap path to uninstall.
 * Opened from the hamburger menu. Uninstall goes through Android's own
 * confirmation dialog (the platform does not allow silent uninstalls).
 */
class InstalledAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstalledBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: InstalledAppsAdapter
    private var allApps: List<InstalledAppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstalledBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        adapter = InstalledAppsAdapter(onUninstall = { app -> uninstall(app) })
        binding.installedList.layoutManager = LinearLayoutManager(this)
        binding.installedList.adapter = adapter

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })
        binding.goalBtn.setOnClickListener { editGoal() }
        binding.backBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        binding.installedEmpty.text = "Loading apps…"
        binding.installedEmpty.visibility = View.VISIBLE
        Thread {
            val apps = InstalledApps.load(packageManager)
            runOnUiThread {
                allApps = apps
                applyFilter()
                refreshHeader()
            }
        }.start()
    }

    private fun refreshHeader() {
        val count = allApps.size
        val goal = prefs.appCountGoal
        binding.installedSummary.text =
            "$count apps installed · ${InstalledApps.goalLine(count, goal)}"
    }

    private fun applyFilter() {
        val shown = InstalledApps.filterByQuery(allApps, binding.searchBox.text.toString())
        adapter.submit(shown)
        binding.installedEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        if (shown.isEmpty() && allApps.isNotEmpty()) {
            binding.installedEmpty.text = "No apps match your search."
        }
    }

    private fun editGoal() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(prefs.appCountGoal.toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("App count goal")
            .setMessage("The home-screen widget shows how close you are to this number.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val goal = input.text.toString().toIntOrNull()
                if (goal == null || goal < 1) {
                    Toast.makeText(this, "Enter a number of 1 or more", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.appCountGoal = goal
                refreshHeader()
                AppCountWidget.updateAll(this)
                Toast.makeText(this, "Goal set to $goal apps", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uninstall(app: InstalledAppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}"))
        prefs.addEvent("Uninstall requested: ${app.name} (${app.pkg})")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            prefs.addEvent("Uninstall failed to open: ${e.message}")
            Toast.makeText(this, "Couldn't open the uninstaller", Toast.LENGTH_LONG).show()
        }
    }
}
