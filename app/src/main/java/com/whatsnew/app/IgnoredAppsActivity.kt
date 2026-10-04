package com.whatsnew.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsnew.app.databinding.ActivityIgnoredBinding

/**
 * The ignore list, moved out of the home screen.
 * Opened from the hamburger menu.
 */
class IgnoredAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIgnoredBinding
    private lateinit var prefs: Prefs
    private lateinit var adapter: IgnoredAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIgnoredBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        adapter = IgnoredAppsAdapter(
            onRemove = { pkg ->
                prefs.setIgnored(pkg, false)
                refresh()
                Toast.makeText(this, "Removed from ignore list", Toast.LENGTH_SHORT).show()
            },
            labelFor = { pkg -> appLabel(pkg) }
        )
        binding.ignoredList.layoutManager = LinearLayoutManager(this)
        binding.ignoredList.adapter = adapter

        binding.backBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
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
