package com.romoreno.malagapp.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.romoreno.malagapp.R
import com.romoreno.malagapp.data.database.entities.CameraEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.coroutineContext

/**
 * Video de un rango de dias a partir del historico de imagenes que publica el servidor de la
 * camara (una carpeta por dia con fotos cada 15 min).
 *
 * Para no llenar el movil las fotos NO se guardan: se descargan una a una, se meten en el video
 * y se descartan. Solo queda el MP4 final, que ademas se borra de la cache tras reproducirlo.
 */
object HistoryVideo {

    /** Primer dia con historico en el servidor (2023-01-02, en milisegundos UTC). */
    private const val FIRST_DAY_UTC = 1_672_617_600_000L
    private const val DAY_MS = 86_400_000L
    private const val MAX_DAYS = 31
    /** Tope de fotos por video: si el rango da mas, se usa una de cada k. */
    private const val MAX_FRAMES = 240
    private const val HISTORY_FPS = 8

    private val FILE_REGEX = Regex("""href="(\d{14})\.jpg"""")

    fun show(
        context: Context,
        owner: LifecycleOwner,
        camera: CameraEntity,
        baseUrl: String
    ) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            Toast.makeText(context, R.string.record_error, Toast.LENGTH_LONG).show()
            return
        }
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraints = CalendarConstraints.Builder()
            .setStart(FIRST_DAY_UTC)
            .setEnd(today)
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf(
                        DateValidatorPointForward.from(FIRST_DAY_UTC),
                        DateValidatorPointBackward.before(today + DAY_MS)
                    )
                )
            )
            .build()
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.history_pick_range)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val first = range.first ?: return@addOnPositiveButtonClickListener
            val second = range.second ?: first
            generate(activity, owner, camera, baseUrl, first, second)
        }
        picker.show(activity.supportFragmentManager, "history_range")
    }

    private fun generate(
        activity: FragmentActivity,
        owner: LifecycleOwner,
        camera: CameraEntity,
        baseUrl: String,
        fromUtc: Long,
        toUtc: Long
    ) {
        val days = ((toUtc - fromUtc) / DAY_MS).toInt() + 1
        if (days > MAX_DAYS) {
            Toast.makeText(
                activity, activity.getString(R.string.history_too_long, MAX_DAYS), Toast.LENGTH_LONG
            ).show()
            return
        }

        // Dialogo de progreso con boton de cancelar
        val text = TextView(activity).apply { setText(R.string.history_searching) }
        val bar = LinearProgressIndicator(activity).apply { isIndeterminate = true }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad / 2)
            addView(text, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(
                bar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = pad / 2 }
            )
        }
        var job: Job? = null
        val progress = AlertDialog.Builder(activity)
            .setTitle(R.string.history_title)
            .setView(content)
            .setCancelable(false)
            .setNegativeButton(R.string.alias_cancel) { _, _ -> job?.cancel() }
            .show()

        job = owner.lifecycleScope.launch {
            try {
                // 1) Que fotos hay en esos dias
                val names = withContext(Dispatchers.IO) { listFrames(baseUrl, fromUtc, days) }
                if (names.size < 2) {
                    Toast.makeText(activity, R.string.history_none, Toast.LENGTH_LONG).show()
                    return@launch
                }
                val chosen = sample(names, MAX_FRAMES)
                bar.isIndeterminate = false
                bar.max = chosen.size
                val step = if (chosen.size < names.size) names.size / chosen.size.toFloat() else 1f

                // 2) Descarga + codificacion en streaming (una foto a la vez)
                val file = withContext(Dispatchers.IO) {
                    val job = coroutineContext[Job]
                    val dir = File(activity.cacheDir, "shared").apply { mkdirs() }
                    val out = File(dir, "historia_${camera.id}.mp4")
                    val ok = FrameVideoEncoder.encodeStreaming(
                        count = chosen.size,
                        out = out,
                        fps = HISTORY_FPS,
                        isCancelled = { job?.isActive == false },
                        onProgress = { done ->
                            activity.runOnUiThread {
                                bar.setProgressCompat(done, true)
                                text.text = activity.getString(
                                    R.string.history_progress, done, chosen.size
                                ) + if (step > 1f) "\n" + activity.getString(
                                    R.string.history_sampled, step.toInt()
                                ) else ""
                            }
                        }
                    ) { i ->
                        val name = chosen[i]
                        download("$baseUrl/${name.substring(0, 8)}/$name.jpg")
                            ?.let { EncFrame(it, label(name)) }
                    }
                    if (ok) out else {
                        out.delete() // cancelado o fallido: sin restos
                        null
                    }
                }
                if (file == null) {
                    Toast.makeText(activity, R.string.record_error, Toast.LENGTH_LONG).show()
                    return@launch
                }
                progress.dismiss()
                CameraDialog.saveAndPlay(activity, camera, file)
            } finally {
                if (progress.isShowing) progress.dismiss()
            }
        }
    }

    /** Nombres (sin extension) de las fotos de [days] dias desde [fromUtc], ordenados. */
    private fun listFrames(baseUrl: String, fromUtc: Long, days: Int): List<String> {
        val format = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = fromUtc
        }
        val result = mutableListOf<String>()
        repeat(days) {
            val day = format.format(calendar.time)
            val html = downloadText("$baseUrl/$day/")
            if (html != null) {
                FILE_REGEX.findAll(html).forEach { result.add(it.groupValues[1]) }
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return result.sorted()
    }

    /** Reparte [max] elementos de forma uniforme si hay mas de los permitidos. */
    private fun sample(list: List<String>, max: Int): List<String> {
        if (list.size <= max) return list
        return List(max) { i -> list[(i.toLong() * (list.size - 1) / (max - 1)).toInt()] }
    }

    /** 20261114001801 -> "14/11/2026 00:18" */
    private fun label(name: String) =
        "${name.substring(6, 8)}/${name.substring(4, 6)}/${name.substring(0, 4)} " +
                "${name.substring(8, 10)}:${name.substring(10, 12)}"

    private fun download(url: String): ByteArray? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.inputStream.use { it.readBytes() }.also { connection.disconnect() }
    } catch (e: Exception) {
        null
    }

    private fun downloadText(url: String): String? = download(url)?.toString(Charsets.UTF_8)
}
