package com.whatsnew.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.whatsnew.app.databinding.ActivityLogsBinding

/**
 * Full-screen, scrollable view of the watcher's activity log.
 * Opened from the hamburger menu on the main screen.
 */
class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.copyLogsBtn.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(
                ClipData.newPlainText("What's New activity log", prefs.eventLog())
            )
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        binding.clearLogsBtn.setOnClickListener {
            prefs.clearLog()
            refresh()
            Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show()
        }
        binding.backBtn.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        binding.logsText.text = prefs.eventLog().ifBlank { "Log is empty." }
        // Newest entries are appended at the bottom — jump there.
        binding.logsScroll.post {
            binding.logsScroll.fullScroll(View.FOCUS_DOWN)
        }
    }
}
