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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.getstream.photoview.PhotoView
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
import com.romoreno.malagapp.data.database.provider.ExtraCameras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Dialogo con la foto de una camara.
 *  - "Actualizar": recarga la foto al momento y, mientras esta activo (relleno), cada 5 s.
 *  - Compartir: envia la foto (imagen) + texto a otras apps.
 *  - Guardar: guarda la foto en la galeria (Pictures/Malagapp).
 */
object CameraDialog {

    private const val REFRESH_MILLIS = 5000L
    private const val RECORD_POLL_MILLIS = 2000L
    private const val MAX_FRAMES = 90

    /**
     * @param onFavouriteToggled si no es null se muestra la estrella de favorito; se invoca con
     * la camara tal y como estaba antes de pulsar (la misma que espera markCameraAsFavourite).
     */
    fun show(
        context: Context,
        owner: LifecycleOwner,
        camera: CameraEntity,
        onFavouriteToggled: ((CameraEntity) -> Unit)? = null
    ) {
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

        // La caja de la foto se adapta a la proporcion de cada imagen (4:3 en las del
        // Ayuntamiento, 16:9 en la de Nueva Malaga) en vez de fijarse a 4:3
        val applyRatio: (Int, Int) -> Unit = { w, h ->
            if (w > 0 && h > 0) {
                val params = photoView.layoutParams as ConstraintLayout.LayoutParams
                val ratio = "$w:$h"
                if (params.dimensionRatio != ratio) {
                    params.dimensionRatio = ratio
                    photoView.layoutParams = params
                }
            }
        }

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
                        loadPhoto(context, camera.url, photoView, applyRatio)
                        delay(REFRESH_MILLIS)
                    }
                }
            }
        }

        if (onFavouriteToggled != null) {
            val star = dialog.findViewById<MaterialButton>(R.id.btnFavourite)
            var current = camera
            fun renderStar() = star.setIconResource(
                if (current.favourite) R.drawable.ic_filled_star else R.drawable.ic_star
            )
            star.visibility = android.view.View.VISIBLE
            renderStar()
            star.setOnClickListener {
                val before = current
                current = before.copy(favourite = !before.favourite)
                renderStar()
                onFavouriteToggled(before)
            }
        }

        share.setOnClickListener { sharePhoto(context, owner, camera, photoView) }
        photoView.setOnLongClickListener {
            sharePhoto(context, owner, camera, photoView)
            true
        }
        save.setOnClickListener { savePhoto(context, owner, camera, photoView) }

        // Grabacion: cada RECORD_POLL_MILLIS se descarga la imagen y solo se guarda si es nueva
        // (el servidor publica un fotograma nuevo cada ~6 s, asi no hay duplicados)
        val record = dialog.findViewById<MaterialButton>(R.id.btnRecord)
        val frames = mutableListOf<ByteArray>()
        var recordJob: Job? = null

        fun renderRecord(active: Boolean) {
            val error = MaterialColors.getColor(record, androidx.appcompat.R.attr.colorError)
            val onError =
                MaterialColors.getColor(record, com.google.android.material.R.attr.colorOnError)
            val primary = MaterialColors.getColor(record, androidx.appcompat.R.attr.colorPrimary)
            val textColor = if (active) onError else primary
            record.text = if (active) context.getString(R.string.record_stop, frames.size)
            else context.getString(R.string.record_start)
            record.backgroundTintList =
                ColorStateList.valueOf(if (active) error else Color.TRANSPARENT)
            record.setTextColor(textColor)
            record.iconTint = ColorStateList.valueOf(textColor)
        }

        fun finishRecording() {
            recordJob?.cancel()
            recordJob = null
            val captured = frames.toList()
            frames.clear()
            renderRecord(false)
            createVideo(context, owner, camera, captured)
        }

        record.setOnClickListener {
            if (recordJob?.isActive == true) {
                finishRecording()
            } else {
                frames.clear()
                renderRecord(true)
                recordJob = owner.lifecycleScope.launch {
                    while (isActive && frames.size < MAX_FRAMES) {
                        val bytes = withContext(Dispatchers.IO) { download(camera.url) }
                        if (bytes != null && (frames.isEmpty() || !bytes.contentEquals(frames.last()))) {
                            frames.add(bytes)
                            renderRecord(true)
                        }
                        delay(RECORD_POLL_MILLIS)
                    }
                    if (frames.size >= MAX_FRAMES) finishRecording()
                }
            }
        }

        // Solo las camaras con historico en su servidor muestran este boton
        ExtraCameras.HISTORY_BASE_URL[camera.id]?.let { baseUrl ->
            val history = dialog.findViewById<MaterialButton>(R.id.btnHistory)
            history.visibility = android.view.View.VISIBLE
            history.setOnClickListener { HistoryVideo.show(context, owner, camera, baseUrl) }
        }

        dialog.setOnDismissListener {
            refreshJob?.cancel()
            // Si se cierra grabando, se crea el video con lo capturado hasta ahora
            if (recordJob?.isActive == true) finishRecording()
        }

        // Doble toque: foto a pantalla completa en apaisado, con zoom por pellizco
        (photoView as PhotoView).setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                FullScreenPhoto.show(context, owner, camera.url, photoView.drawable)
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent) = false
            override fun onSingleTapConfirmed(e: MotionEvent) = false
        })

        loadPhoto(context, camera.url, photoView, applyRatio)
        dialog.show()
    }

    /** Carga la foto sin cache; conserva la anterior mientras llega la nueva (sin parpadeo). */
    fun loadPhoto(
        context: Context,
        url: String,
        photoView: ImageView,
        onLoaded: ((width: Int, height: Int) -> Unit)? = null
    ) {
        Glide.with(context)
            .load(url)
            .placeholder(photoView.drawable)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                ) = false

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    onLoaded?.invoke(resource.intrinsicWidth, resource.intrinsicHeight)
                    return false
                }
            })
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

    private fun download(url: String): ByteArray? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.inputStream.use { it.readBytes() }.also { connection.disconnect() }
    } catch (e: Exception) {
        null
    }

    /** Codifica los fotogramas a MP4, lo guarda en la galeria y lo abre para verlo. */
    private fun createVideo(
        context: Context,
        owner: LifecycleOwner,
        camera: CameraEntity,
        frames: List<ByteArray>
    ) {
        if (frames.size < 2) {
            Toast.makeText(context, R.string.record_few, Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(context, R.string.record_creating, Toast.LENGTH_SHORT).show()
        owner.lifecycleScope.launch {
            val file = withContext(Dispatchers.Default) {
                val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                val out = File(dir, "camara_${camera.id}.mp4")
                if (FrameVideoEncoder.encode(frames, out)) out else {
                    out.delete() // sin restos de un video a medias
                    null
                }
            }
            if (file == null) {
                Toast.makeText(context, R.string.record_error, Toast.LENGTH_LONG).show()
                return@launch
            }
            saveAndPlay(context, camera, file)
        }
    }

    /**
     * Guarda el MP4 en la galeria y lo reproduce con el reproductor propio (el de MP4 por
     * defecto del movil puede no ser de video). La copia de la cache se borra SIEMPRE al cerrar
     * el reproductor, se haya podido guardar en la galeria o no: no se deja nada huerfano.
     */
    suspend fun saveAndPlay(context: Context, camera: CameraEntity, file: File) {
        val saved = withContext(Dispatchers.IO) { saveVideoToGallery(context, camera, file) }
        Toast.makeText(
            context,
            if (saved) R.string.record_saved else R.string.record_not_saved,
            Toast.LENGTH_LONG
        ).show()
        try {
            VideoPlayerDialog.show(context, Uri.fromFile(file)) { file.delete() }
        } catch (e: Exception) {
            file.delete()
        }
    }

    private fun saveVideoToGallery(context: Context, camera: CameraEntity, source: File): Boolean {
        val name = "malagapp_${camera.id}_${System.currentTimeMillis()}.mp4"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Malagapp")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                } ?: return false
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) return false
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "Malagapp"
                ).apply { mkdirs() }
                val target = File(dir, name)
                source.copyTo(target, overwrite = true)
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(target.absolutePath), arrayOf("video/mp4"), null
                )
                true
            }
        } catch (e: Exception) {
            false
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
