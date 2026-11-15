package com.romoreno.malagapp.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.romoreno.malagapp.R
import com.romoreno.malagapp.databinding.FragmentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Galeria interna: lista las fotos (Pictures/Malagapp) y videos (Movies/Malagapp) que la app ha
 * guardado. Toque = abrir, pulsacion larga = compartir o eliminar.
 */
class GalleryFragment : Fragment() {

    private enum class Filter { ALL, PHOTOS, VIDEOS }

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: GalleryAdapter
    private var all: List<MediaItem> = emptyList()
    private var filter = Filter.ALL

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = GalleryAdapter(onClick = { open(it) }, onLongClick = { showActions(it) })
        binding.rvMedia.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvMedia.adapter = adapter

        binding.filterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            filter = when (checkedId) {
                R.id.btnPhotos -> Filter.PHOTOS
                R.id.btnVideos -> Filter.VIDEOS
                else -> Filter.ALL
            }
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            binding.tvEmpty.setText(R.string.gallery_unsupported)
            binding.tvEmpty.isVisible = true
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            all = withContext(Dispatchers.IO) { queryMedia(requireContext()) }
            _binding?.let { render() }
        }
    }

    private fun render() {
        val list = when (filter) {
            Filter.ALL -> all
            Filter.PHOTOS -> all.filter { !it.isVideo }
            Filter.VIDEOS -> all.filter { it.isVideo }
        }
        adapter.submitList(list)
        binding.tvEmpty.setText(R.string.gallery_empty)
        binding.tvEmpty.isVisible = list.isEmpty()
    }

    private fun open(item: MediaItem) {
        if (item.isVideo) {
            VideoPlayerDialog.show(requireContext(), item.uri)
            return
        }
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(item.uri, if (item.isVideo) "video/mp4" else "image/jpeg")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.record_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActions(item: MediaItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(item.name)
            .setItems(
                arrayOf(getString(R.string.share), getString(R.string.gallery_delete))
            ) { _, which ->
                if (which == 0) share(item) else confirmDelete(item)
            }
            .show()
    }

    private fun share(item: MediaItem) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun confirmDelete(item: MediaItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.gallery_delete_title)
            .setPositiveButton(R.string.gallery_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            requireContext().contentResolver.delete(item.uri, null, null) > 0
                        } catch (e: Exception) {
                            false
                        }
                    }
                    Toast.makeText(
                        requireContext(),
                        if (ok) R.string.gallery_deleted else R.string.gallery_delete_error,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (ok) reload()
                }
            }
            .setNegativeButton(R.string.alias_cancel, null)
            .show()
    }

    /** Fotos y videos guardados por la app (carpeta Malagapp), los mas recientes primero. */
    private fun queryMedia(context: Context): List<MediaItem> {
        val files = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DISPLAY_NAME
        )
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND " +
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
            "%/Malagapp/%"
        )
        val result = mutableListOf<MediaItem>()
        context.contentResolver.query(
            files, projection, selection, args,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isVideo =
                    cursor.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val base = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                result.add(
                    MediaItem(
                        uri = ContentUris.withAppendedId(base, id),
                        isVideo = isVideo,
                        dateAddedSeconds = cursor.getLong(2),
                        name = cursor.getString(3) ?: ""
                    )
                )
            }
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
