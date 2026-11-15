package com.romoreno.malagapp.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.romoreno.malagapp.databinding.ItemMediaBinding
import java.text.DateFormat
import java.util.Date

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val dateAddedSeconds: Long,
    val name: String
)

class GalleryAdapter(
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, GalleryAdapter.Holder>(Diff) {

    class Holder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding
        // Glide saca la miniatura tanto de fotos como de un fotograma del video
        Glide.with(binding.root).load(item.uri).centerCrop().into(binding.ivThumb)
        binding.ivPlay.isVisible = item.isVideo
        binding.tvDate.text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(item.dateAddedSeconds * 1000))
        binding.root.setOnClickListener { onClick(item) }
        binding.root.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    private object Diff : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(a: MediaItem, b: MediaItem) = a.uri == b.uri
        override fun areContentsTheSame(a: MediaItem, b: MediaItem) = a == b
    }
}
