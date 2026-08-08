package com.github.livingwithhippos.unchained.lists.view

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.WebDownloadItem
import com.github.livingwithhippos.unchained.databinding.ItemListDownloadBinding
import com.github.livingwithhippos.unchained.utilities.extension.getFileSizeString
import com.github.livingwithhippos.unchained.utilities.extension.getStatusTranslation

class DownloadListPagingAdapter(private val listener: DownloadListListener) :
    PagingDataAdapter<WebDownloadItem, DownloadViewHolder>(DiffCallback()) {

    var tracker: SelectionTracker<WebDownloadItem>? = null

    class DiffCallback : DiffUtil.ItemCallback<WebDownloadItem>() {
        override fun areItemsTheSame(oldItem: WebDownloadItem, newItem: WebDownloadItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: WebDownloadItem,
            newItem: WebDownloadItem,
        ): Boolean =
            oldItem.downloadState == newItem.downloadState && oldItem.progress == newItem.progress
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding =
            ItemListDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bindCell(item, tracker?.isSelected(item) ?: false)
        }
    }

    override fun getItemViewType(position: Int) = R.layout.item_list_download

    fun getDownloadItem(position: Int): WebDownloadItem? {
        // snapshot().items[position]
        return super.getItem(position)
    }

    fun getPosition(id: Long) = snapshot().indexOfFirst { it?.id == id }
}

class DownloadViewHolder(
    private val binding: ItemListDownloadBinding,
    private val listener: DownloadListListener,
) : RecyclerView.ViewHolder(binding.root) {

    var mItem: WebDownloadItem? = null

    fun bindCell(item: WebDownloadItem, selected: Boolean) {
        mItem = item
        binding.tvTitle.text = itemView.context.getStatusTranslation(item.downloadState)
        binding.tvName.text = item.name
        binding.tvSize.text = getFileSizeString(itemView.context, item.size)
        binding.selectionIndicator.visibility = if (selected) View.VISIBLE else View.GONE
        binding.cvDownload.setOnClickListener { listener.onClick(item) }
    }

    fun getItemDetails(): ItemDetailsLookup.ItemDetails<WebDownloadItem> =
        object : ItemDetailsLookup.ItemDetails<WebDownloadItem>() {
            override fun getPosition(): Int = layoutPosition

            override fun getSelectionKey(): WebDownloadItem? = mItem
        }
}

class DownloadDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<WebDownloadItem>() {
    override fun getItemDetails(event: MotionEvent): ItemDetails<WebDownloadItem>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (recyclerView.getChildViewHolder(view) as DownloadViewHolder).getItemDetails()
        }
        return null
    }
}

interface DownloadListListener {
    fun onClick(item: WebDownloadItem)
}

class DownloadKeyProvider(private val adapter: DownloadListPagingAdapter) :
    ItemKeyProvider<WebDownloadItem>(SCOPE_MAPPED) {
    override fun getKey(position: Int): WebDownloadItem? {
        return adapter.getDownloadItem(position)
    }

    override fun getPosition(key: WebDownloadItem): Int {
        return adapter.getPosition(key.id)
    }
}
