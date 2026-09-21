package com.whatsnew.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.whatsnew.app.databinding.ItemIgnoredAppBinding

class IgnoredAppsAdapter(
    private val onRemove: (String) -> Unit,
    private val labelFor: (String) -> String
) : RecyclerView.Adapter<IgnoredAppsAdapter.VH>() {

    private var items: List<String> = emptyList()

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemIgnoredAppBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemIgnoredAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = items[position]
        holder.b.appName.text = labelFor(pkg)
        holder.b.appPkg.text = pkg
        holder.b.removeBtn.setOnClickListener { onRemove(pkg) }
    }

    override fun getItemCount(): Int = items.size
}
