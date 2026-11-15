package com.romoreno.malagapp.ui

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.entities.CameraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Dialogo con la foto de una camara.
 *  - "Actualizar": recarga la foto al momento y, mientras esta activo (relleno), cada 5 s.
 *  - Compartir: envia la foto (imagen) + texto a otras apps.
 *  - Guardar: guarda la foto en la galeria (Pictures/Malagapp).
 */
object CameraDialog {

    private const val REFRESH_MILLIS = 5000L

    fun show(context: Context, owner: LifecycleOwner, camera: CameraEntity) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_camera)
        // Ancho adaptado a la pantalla (92%), sea cual sea el movil
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val photoView = dialog.findViewById<ImageView>(R.id.photoView)
        val title = dialog.findViewById<MaterialTextView>(R.id.etTitle)
        val refresh = dialog.findViewById<MaterialButton>(R.id.btnRefresh)
        val share = dialog.findViewById<MaterialButton>(R.id.btnShare)
        val save = dialog.findViewById<MaterialButton>(R.id.btnSave)

        title.text = buildString {
            if (!camera.alias.isNullOrBlank()) append(camera.alias).append('\n')
            append(camera.description).append('\n').append(camera.address.trim())
        }
        var refreshJob: Job? = null

        // Relleno cuando la actualizacion automatica esta activa, para que se note el estado
        fun render(active: Boolean) {
            val primary = MaterialColors.getColor(refresh, androidx.appcompat.R.attr.colorPrimary)
            val onPrimary =
                MaterialColors.getColor(refresh, com.google.android.material.R.attr.colorOnPrimary)
            val textColor = if (active) onPrimary else primary
            refresh.setText(if (active) R.string.refresh_stop else R.string.refresh_start)
            refresh.backgroundTintList =
                ColorStateList.valueOf(if (active) primary else Color.TRANSPARENT)
            refresh.setTextColor(textColor)
            refresh.iconTint = ColorStateList.valueOf(textColor)
        }

        refresh.setOnClickListener {
            if (refreshJob?.isActive == true) {
                refreshJob?.cancel()
                refreshJob = null
                render(false)
            } else {
                render(true)
                refreshJob = owner.lifecycleScope.launch {
                    while (isActive) {
                        loadPhoto(context, camera.url, photoView)
                        delay(REFRESH_MILLIS)
                    }
                }
            }
        }

        share.setOnClickListener { sharePhoto(context, owner, camera, photoView) }
        photoView.setOnLongClickListener {
            sharePhoto(context, owner, camera, photoView)
            true
        }
        save.setOnClickListener { savePhoto(context, owner, camera, photoView) }

        dialog.setOnDismissListener { refreshJob?.cancel() }

        loadPhoto(context, camera.url, photoView)
        dialog.show()
    }

    /** Carga la foto sin cache; conserva la anterior mientras llega la nueva (sin parpadeo). */
    fun loadPhoto(context: Context, url: String, photoView: ImageView) {
        Glide.with(context)
            .load(url)
            .placeholder(photoView.drawable)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(photoView)
    }

    private fun currentBitmap(context: Context, photoView: ImageView): Bitmap? {
        val drawable = photoView.drawable
        if (drawable == null || drawable.intrinsicWidth <= 0) {
            Toast.makeText(context, R.string.photo_not_ready, Toast.LENGTH_SHORT).show()
            return null
        }
        return drawable.toBitmap()
    }

    private fun sharePhoto(
        context: Context,
        owner: LifecycleOwner,
        camera: CameraEntity,
        photoView: ImageView
    ) {
        val bitmap = currentBitmap(context, photoView) ?: return
        owner.lifecycleScope.launch {
            val uri = withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, "camara_${camera.id}.jpg")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    null
                }
            }
            if (uri == null) {
                Toast.makeText(context, R.string.photo_save_error, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${camera.description}\n${camera.address.trim()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
        }
    }

    private fun savePhoto(
        context: Context,
        owner: LifecycleOwner,
        camera: CameraEntity,
        photoView: ImageView
    ) {
        val bitmap = currentBitmap(context, photoView) ?: return
        owner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { saveToGallery(context, camera, bitmap) }
            Toast.makeText(
                context,
                if (ok) R.string.photo_saved else R.string.photo_save_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveToGallery(context: Context, camera: CameraEntity, bitmap: Bitmap): Boolean {
        val name = "malagapp_${camera.id}_${System.currentTimeMillis()}.jpg"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Malagapp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                } ?: return false
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                // Android 9 o inferior: hace falta el permiso de almacenamiento
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) return false
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Malagapp"
                ).apply { mkdirs() }
                val file = File(dir, name)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null
                )
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
