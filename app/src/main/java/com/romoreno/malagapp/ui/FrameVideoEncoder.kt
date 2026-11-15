package com.romoreno.malagapp.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Un fotograma JPEG y, opcionalmente, un texto (p. ej. fecha y hora) que se dibuja encima. */
class EncFrame(val bytes: ByteArray, val label: String? = null)

/**
 * Convierte fotogramas JPEG en un MP4 (H.264) sin librerias externas, usando MediaCodec +
 * MediaMuxer. Cada fotograma se pide y se codifica uno a uno, asi que en memoria solo hay un
 * fotograma a la vez, por muchos que tenga el video.
 */
object FrameVideoEncoder {

    private const val MIME = "video/avc"
    private const val DEFAULT_FPS = 4
    private const val MAX_SIDE = 720f
    private const val TIMEOUT_US = 10_000L

    /** Version simple: fotogramas ya en memoria (grabacion en directo). */
    fun encode(frames: List<ByteArray>, out: File): Boolean =
        encodeStreaming(frames.size, out) { EncFrame(frames[it]) }

    /**
     * @param count numero de fotogramas a pedir a [frameAt] (puede devolver null para saltarse uno)
     * @param fps fotogramas por segundo del video resultante
     * @param isCancelled se consulta entre fotogramas; si es true se aborta y devuelve false
     * @param onProgress se llama con el numero de fotogramas ya procesados
     * @return true si el fichero de salida se genero correctamente
     */
    fun encodeStreaming(
        count: Int,
        out: File,
        fps: Int = DEFAULT_FPS,
        isCancelled: () -> Boolean = { false },
        onProgress: (Int) -> Unit = {},
        frameAt: (Int) -> EncFrame?
    ): Boolean {
        // El primer fotograma util fija el tamano del video
        var firstIndex = 0
        var first: EncFrame? = null
        while (firstIndex < count && first == null) {
            if (isCancelled()) return false
            first = frameAt(firstIndex)
            if (first == null) firstIndex++
        }
        if (first == null) return false
        val probe = BitmapFactory.decodeByteArray(first.bytes, 0, first.bytes.size) ?: return false
        val scale = min(1f, MAX_SIDE / max(probe.width, probe.height))
        // Multiplos de 16: los codificadores hardware lo toleran mejor
        val w = max(16, (probe.width * scale).toInt() / 16 * 16)
        val h = max(16, (probe.height * scale).toInt() / 16 * 16)
        probe.recycle()

        val frameUs = 1_000_000L / fps
        val format = MediaFormat.createVideoFormat(MIME, w, h).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            )
            setInteger(MediaFormat.KEY_BIT_RATE, w * h * 6)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        return try {
            out.delete()
            val cod = MediaCodec.createEncoderByType(MIME)
            codec = cod
            cod.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            cod.start()
            val mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux

            val info = MediaCodec.BufferInfo()
            var track = -1

            fun drain(endOfStream: Boolean) {
                var idleTurns = 0
                while (true) {
                    val index = cod.dequeueOutputBuffer(info, TIMEOUT_US)
                    when {
                        index == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) return
                            // Esperando el EOS: damos un margen razonable antes de rendirnos
                            if (++idleTurns > 500) throw IllegalStateException("encoder timeout")
                        }
                        index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            track = mux.addTrack(cod.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                        index >= 0 -> {
                            val buffer = cod.getOutputBuffer(index)!!
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (info.size > 0 && muxerStarted) {
                                buffer.position(info.offset)
                                buffer.limit(info.offset + info.size)
                                mux.writeSampleData(track, buffer, info)
                            }
                            cod.releaseOutputBuffer(index, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                    }
                }
            }

            // Un unico bitmap de trabajo reutilizado para todos los fotogramas
            val work = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(work)
            val dst = Rect(0, 0, w, h)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = h * 0.045f
            }
            val bgPaint = Paint().apply { color = 0xAA000000.toInt() }
            val pixels = IntArray(w * h)
            var queued = 0L

            fun feed(frame: EncFrame): Boolean {
                val source = BitmapFactory.decodeByteArray(frame.bytes, 0, frame.bytes.size)
                    ?: return false
                canvas.drawBitmap(source, null, dst, null)
                source.recycle()
                frame.label?.let {
                    val pad = textPaint.textSize * 0.4f
                    val width = textPaint.measureText(it)
                    canvas.drawRect(0f, h - textPaint.textSize - pad * 2, width + pad * 2, h.toFloat(), bgPaint)
                    canvas.drawText(it, pad, h - pad - textPaint.descent(), textPaint)
                }
                work.getPixels(pixels, 0, w, 0, 0, w, h)

                var inputIndex = cod.dequeueInputBuffer(TIMEOUT_US)
                while (inputIndex < 0) {
                    drain(false)
                    inputIndex = cod.dequeueInputBuffer(TIMEOUT_US)
                }
                fillImage(cod.getInputImage(inputIndex)!!, pixels, w, h)
                cod.queueInputBuffer(inputIndex, 0, w * h * 3 / 2, queued * frameUs, 0)
                queued++
                drain(false)
                return true
            }

            var last: EncFrame? = null
            for (i in firstIndex until count) {
                if (isCancelled()) return false
                val frame = (if (i == firstIndex) first else frameAt(i)) ?: continue
                if (feed(frame)) last = frame
                onProgress(i + 1)
            }
            // El ultimo fotograma se repite para que no desaparezca al instante
            last?.let { feed(it) }
            work.recycle()

            var inputIndex = cod.dequeueInputBuffer(TIMEOUT_US)
            while (inputIndex < 0) {
                drain(false)
                inputIndex = cod.dequeueInputBuffer(TIMEOUT_US)
            }
            cod.queueInputBuffer(
                inputIndex, 0, 0, queued * frameUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
            drain(true)
            muxerStarted && queued >= 2
        } catch (e: Exception) {
            false
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }

    /** ARGB -> YUV420 escribiendo directamente en los planos de la imagen del codificador. */
    private fun fillImage(image: Image, pixels: IntArray, w: Int, h: Int) {
        val (yPlane, uPlane, vPlane) = image.planes.let { Triple(it[0], it[1], it[2]) }
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = pixels[y * w + x]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val luma = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yBuf.put(y * yPlane.rowStride + x * yPlane.pixelStride, luma.coerceIn(0, 255).toByte())
                if (y % 2 == 0 && x % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    uBuf.put(
                        (y / 2) * uPlane.rowStride + (x / 2) * uPlane.pixelStride,
                        u.coerceIn(0, 255).toByte()
                    )
                    vBuf.put(
                        (y / 2) * vPlane.rowStride + (x / 2) * vPlane.pixelStride,
                        v.coerceIn(0, 255).toByte()
                    )
                }
            }
        }
    }
}
