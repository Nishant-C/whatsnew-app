package com.whatsnew.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.whatsnew.app.databinding.OverlayChangelogBinding

/**
 * Transparent activity that shows the changelog card.
 *
 * Second fallback when the system overlay (TYPE_APPLICATION_OVERLAY) is rejected
 * by the device even though the permission looks granted. Apps holding the
 * overlay permission are exempt from background-activity-start restrictions, so
 * the watcher service can open this directly.
 */
class PopupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = OverlayChangelogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val versionName = intent.getStringExtra(EXTRA_VERSION) ?: ""
        val changelog = intent.getStringExtra(EXTRA_CHANGELOG) ?: ""
        val pkg = intent.getStringExtra(EXTRA_PKG) ?: ""

        binding.title.text =
            if (versionName.isNotBlank()) "$appName updated • $versionName"
            else "$appName updated"
        binding.changelog.text = changelog
        binding.dismissBtn.setOnClickListener { finish() }
        binding.scrim.setOnClickListener { finish() }
        binding.ignoreBtn.setOnClickListener {
            if (pkg.isNotBlank()) Prefs(this).setIgnored(pkg, true)
            finish()
        }
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_APP_NAME = "appName"
        private const val EXTRA_VERSION = "version"
        private const val EXTRA_CHANGELOG = "changelog"

        /** Returns false if the activity could not be started. */
        fun show(
            context: Context,
            pkg: String,
            appName: String,
            versionName: String,
            changelog: String
        ): Boolean {
            return try {
                val i = Intent(context, PopupActivity::class.java)
                    .putExtra(EXTRA_PKG, pkg)
                    .putExtra(EXTRA_APP_NAME, appName)
                    .putExtra(EXTRA_VERSION, versionName)
                    .putExtra(EXTRA_CHANGELOG, changelog)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
