package com.whatsnew.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.whatsnew.app.databinding.ItemUpdateBinding

/** One row of the "Latest updates" screen: the last fetched changelog per app. */
data class UpdateInfo(
    val pkg: String,
    val appName: String,
    val versionName: String?,
    val fetchedAt: Long,
    val changelog: String
)

class UpdatesAdapter : RecyclerView.Adapter<UpdatesAdapter.VH>() {

    private var items: List<UpdateInfo> = emptyList()

    fun submit(list: List<UpdateInfo>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemUpdateBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemUpdateBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val info = items[position]
        holder.b.appName.text = info.appName
        holder.b.appMeta.text = UpdateMeta.metaLine(info.versionName, info.fetchedAt)
        holder.b.changelogText.text = info.changelog
    }

    override fun getItemCount(): Int = items.size
}
