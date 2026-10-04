package com.whatsnew.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsnew.app.databinding.ActivityUpdatesBinding

/**
 * Manually browsable record of the latest "What's new" text fetched per app.
 * Opened from the hamburger menu.
 */
class UpdatesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdatesBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: UpdatesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdatesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        adapter = UpdatesAdapter()
        binding.updatesList.layoutManager = LinearLayoutManager(this)
        binding.updatesList.adapter = adapter

        binding.backBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val infos = prefs.changelogPackages().mapNotNull { pkg ->
            val changelog = prefs.lastChangelog(pkg) ?: return@mapNotNull null
            UpdateInfo(
                pkg = pkg,
                appName = appLabel(pkg),
                versionName = prefs.lastVersionName(pkg),
                fetchedAt = prefs.changelogFetchedAt(pkg),
                changelog = changelog
            )
        }
        adapter.submit(infos)
        binding.updatesEmpty.visibility = if (infos.isEmpty()) View.VISIBLE else View.GONE
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
