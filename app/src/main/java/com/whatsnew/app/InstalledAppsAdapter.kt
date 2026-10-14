package com.whatsnew.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.whatsnew.app.databinding.ItemInstalledAppBinding

/** One row of the "Installed apps" screen: icon, name, version/size, Uninstall. */
class InstalledAppsAdapter(
    private val onUninstall: (InstalledAppInfo) -> Unit
) : RecyclerView.Adapter<InstalledAppsAdapter.VH>() {

    private var items: List<InstalledAppInfo> = emptyList()

    fun submit(list: List<InstalledAppInfo>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemInstalledAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemInstalledAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = items[position]
        holder.b.appIcon.setImageDrawable(app.icon)
        holder.b.appName.text = app.name
        val version = app.versionName?.let { "v$it" } ?: ""
        val size = InstalledApps.formatSize(app.sizeBytes)
        holder.b.appMeta.text = listOf(version, size).filter { it.isNotEmpty() }.joinToString(" · ")
        holder.b.uninstallBtn.setOnClickListener { onUninstall(app) }
    }

    override fun getItemCount(): Int = items.size
}
