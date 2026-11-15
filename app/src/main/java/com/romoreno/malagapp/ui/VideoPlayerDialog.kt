package com.romoreno.malagapp.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import com.romoreno.malagapp.R
import java.util.Locale

/**
 * Reproductor de video a pantalla completa dentro de la app (en bucle). Evita depender de la
 * app externa que Android elija: en algunos moviles el reproductor por defecto de MP4 es la app
 * de musica. Controles siempre visibles: pausa, atras, adelante y barra de tiempo desplazable.
 */
object VideoPlayerDialog {

    private const val UPDATE_MILLIS = 100L

    fun show(context: Context, uri: Uri, onDismiss: () -> Unit = {}) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = FrameLayout(context).apply { setBackgroundColor(Color.BLACK) }
        val video = VideoView(context)
        root.addView(
            video,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )

        // --- Controles ---
        val seekBar = SeekBar(context)
        val timeText = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            text = "0:00 / 0:00"
        }
        fun button(icon: Int, description: String) = ImageButton(context).apply {
            setImageResource(icon)
            contentDescription = description
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val back = button(android.R.drawable.ic_media_rew, "Atrás")
        val playPause = button(android.R.drawable.ic_media_pause, "Pausa")
        val forward = button(android.R.drawable.ic_media_ff, "Adelante")

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(playPause)
            addView(forward)
            addView(
                timeText,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(12) }
            )
        }
        val controls = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xAA000000.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(24))
            addView(seekBar)
            addView(buttons)
        }
        root.addView(
            controls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        // --- Logica ---
        val handler = Handler(Looper.getMainLooper())
        var player: MediaPlayer? = null
        var dragging = false

        fun format(ms: Int): String {
            val total = ms / 1000
            return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
        }

        fun seek(positionMs: Int) {
            val target = positionMs.coerceIn(0, video.duration.coerceAtLeast(0))
            // SEEK_CLOSEST va al fotograma exacto, no al clave mas cercano (API 26+)
            val mp = player
            if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                video.seekTo(target)
            }
        }

        fun refresh() {
            if (!dragging) {
                seekBar.progress = video.currentPosition
                timeText.text = "${format(video.currentPosition)} / ${format(video.duration)}"
            }
            playPause.setImageResource(
                if (video.isPlaying) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play
            )
        }

        val ticker = object : Runnable {
            override fun run() {
                refresh()
                handler.postDelayed(this, UPDATE_MILLIS)
            }
        }

        // Paso de los botones atras/adelante proporcional a la duracion (entre 0,25 y 2 s)
        fun step() = (video.duration / 10).coerceIn(250, 2000)

        playPause.setOnClickListener {
            if (video.isPlaying) video.pause() else video.start()
            refresh()
        }
        back.setOnClickListener { seek(video.currentPosition - step()); refresh() }
        forward.setOnClickListener { seek(video.currentPosition + step()); refresh() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seek(progress)
                    timeText.text = "${format(progress)} / ${format(video.duration)}"
                }
            }

            override fun onStartTrackingTouch(bar: SeekBar) {
                dragging = true
            }

            override fun onStopTrackingTouch(bar: SeekBar) {
                dragging = false
            }
        })

        video.setOnPreparedListener { mp ->
            player = mp
            mp.isLooping = true
            seekBar.max = video.duration
            video.start()
            handler.post(ticker)
        }
        video.setOnErrorListener { _, _, _ ->
            Toast.makeText(context, R.string.record_error, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            true
        }
        dialog.setOnDismissListener {
            handler.removeCallbacks(ticker)
            video.stopPlayback()
            player = null
            onDismiss()
        }
        dialog.setContentView(root)
        video.setVideoURI(uri)
        dialog.show()
    }
}
