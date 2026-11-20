package com.mx.mountain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.*

enum class RouteMapMode { SATELLITE, TERRAIN }

object RouteVideoEncoder {
    private const val WIDTH = 540
    private const val HEIGHT = 960
    private const val FRAME_RATE = 30

    fun encodeWithMapBackground(
        context: Context,
        output: File,
        mapBitmap: Bitmap,
        points: List<RoutePoint>,
        callback: (File?, Exception?) -> Unit,
        statisticsText: String = ""
    ) {
        encode(context, output, points, emptyList(), RouteMapMode.SATELLITE, callback, mapBitmap, statisticsText)
    }

    fun encode(
        context: Context,
        output: File,
        points: List<RoutePoint>,
        photos: List<Uri>,
        mapMode: RouteMapMode,
        callback: (File?, Exception?) -> Unit,
        mapBitmap: Bitmap? = null,
        statisticsText: String = ""
    ) {
        Thread {
            var encoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            try {
                val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                    setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }

                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

                val info = MediaCodec.BufferInfo()
                muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                encoder.start()

                var trackIndex = -1
                var muxerStarted = false
                val timeoutUs = 10_000L
                val frameCount = 45

                for (frame in 0 until frameCount) {
                    val inputIndex = encoder.dequeueInputBuffer(timeoutUs)
                    if (inputIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputIndex)
                        inputBuffer?.clear()
                        val bitmap = if (mapBitmap != null)
                            renderMapFrame(mapBitmap, points, frame / (frameCount - 1f), statisticsText)
                        else
                            renderFrame(points, photos, frame / (frameCount - 1f), mapMode)
                        val yuv = argbToYuv420(bitmap)
                        inputBuffer?.put(yuv)
                        val presentationTimeUs = frame * 1_000_000L / FRAME_RATE
                        encoder.queueInputBuffer(inputIndex, 0, yuv.size, presentationTimeUs, 0)
                        bitmap.recycle()
                    }

                    var outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                    while (outputIndex >= 0) {
                        if (!muxerStarted) {
                            if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                                continue
                            }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        val outputBuffer = encoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && info.size > 0) {
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            muxer.writeSampleData(trackIndex, outputBuffer, info)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                        outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                    }
                }

                val endIndex = encoder.dequeueInputBuffer(timeoutUs)
                if (endIndex >= 0) {
                    encoder.queueInputBuffer(endIndex, 0, 0, frameCount * 1_000_000L / FRAME_RATE, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }

                var eos = false
                while (!eos) {
                    val outputIndex = encoder.dequeueOutputBuffer(info, timeoutUs)
                    if (outputIndex >= 0) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        if (info.size > 0) {
                            val outputBuffer = encoder.getOutputBuffer(outputIndex)
                            if (outputBuffer != null) muxer.writeSampleData(trackIndex, outputBuffer, info)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) eos = true
                    }
                }

                muxer.stop()
                muxer.release()
                muxer = null
                encoder.stop()
                encoder.release()
                encoder = null
                callback(output, null)
            } catch (error: Exception) {
                try { encoder?.stop() } catch (_: Exception) {}
                try { encoder?.release() } catch (_: Exception) {}
                try { muxer?.release() } catch (_: Exception) {}
                callback(null, error)
            }
        }.start()
    }

    private fun renderMapFrame(mapBitmap: Bitmap, points: List<RoutePoint>, progress: Float, statisticsText: String): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val dest = android.graphics.RectF(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
        val source = android.graphics.Rect(0, 0, mapBitmap.width, mapBitmap.height)
        canvas.drawBitmap(mapBitmap, source, dest, Paint(Paint.FILTER_BITMAP_FLAG))

        val overlay = Paint().apply { color = Color.argb(40, 0, 0, 0) }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), overlay)

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.000001)
        val padding = 70f

        fun position(index: Int): Pair<Float, Float> {
            val point = points[index]
            val x = padding + (((point.longitude - minLon) / lonRange) * (WIDTH - padding * 2)).toFloat()
            val y = (HEIGHT - 180f) - (((point.latitude - minLat) / latRange) * (HEIGHT - 360f)).toFloat()
            return x to y
        }

        val route = Path()
        val animated = Path()
        points.forEachIndexed { index, _ ->
            val (x, y) = position(index)
            if (index == 0) route.moveTo(x, y) else route.lineTo(x, y)
        }
        val visibleCount = (points.size * progress).toInt().coerceAtLeast(1)
        repeat(visibleCount) { index ->
            val (x, y) = position(index)
            if (index == 0) animated.moveTo(x, y) else animated.lineTo(x, y)
        }

        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 18f; color = Color.argb(180, 0, 35, 20) }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 10f; color = Color.rgb(255, 193, 7); strokeCap = Paint.Cap.ROUND }
        val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.rgb(255, 193, 7) }
        canvas.drawPath(route, shadow)
        canvas.drawPath(animated, line)

        val (startX, startY) = position(0)
        val (currentX, currentY) = position((visibleCount - 1).coerceIn(0, points.lastIndex))
        canvas.drawCircle(startX, startY, 14f, marker)
        canvas.drawCircle(currentX, currentY, 18f, marker)

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 46f; setShadowLayer(8f, 0f, 2f, Color.BLACK) }
        canvas.drawText("RUTA 3D · SATÉLITE", 48f, 82f, title)

        if (progress >= 0.82f && statisticsText.isNotBlank()) {
            val cardTop = HEIGHT - 310f
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 12, 24, 18) }
            canvas.drawRoundRect(42f, cardTop, WIDTH - 42f, HEIGHT - 55f, 24f, 24f, cardPaint)

            val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                setShadowLayer(5f, 0f, 2f, Color.BLACK)
            }
            var lineY = cardTop + 58f
            statisticsText.split('\n').forEach { line ->
                canvas.drawText(line, 70f, lineY, statsPaint)
                lineY += 42f
            }
        }
        return bitmap
    }

    private fun renderFrame(points: List<RoutePoint>, photos: List<Uri>, progress: Float, mode: RouteMapMode): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(if (mode == RouteMapMode.SATELLITE) Color.rgb(42, 54, 48) else Color.rgb(232, 241, 232))

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mode == RouteMapMode.SATELLITE) Color.argb(55, 180, 210, 185) else Color.argb(35, 80, 120, 80)
            strokeWidth = 2f
        }
        if (mode == RouteMapMode.SATELLITE) {
            var y = 0f
            while (y < HEIGHT) { canvas.drawLine(0f, y, WIDTH.toFloat(), y, grid); y += 48f }
            var x = 0f
            while (x < WIDTH) { canvas.drawLine(x, 0f, x, HEIGHT.toFloat(), grid); x += 48f }
        }

        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 54f }
        canvas.drawText("RUTA 3D", 50f, 90f, title)

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.000001)
        val padding = 90f

        fun position(index: Int): Pair<Float, Float> {
            val point = points[index]
            val x = padding + (((point.longitude - minLon) / lonRange) * (WIDTH - padding * 2)).toFloat()
            val y = (HEIGHT - 220f) - (((point.latitude - minLat) / latRange) * (HEIGHT - 440f)).toFloat()
            val elevation = point.elevation?.toFloat() ?: 0f
            return x to y - elevation / 10000f * 40f
        }

        val route = Path()
        points.forEachIndexed { index, _ ->
            val (x, y) = position(index)
            if (index == 0) route.moveTo(x, y) else route.lineTo(x, y)
        }

        val visibleCount = (points.size * progress).toInt().coerceAtLeast(1)
        val animated = Path()
        repeat(visibleCount) { index ->
            val (x, y) = position(index)
            if (index == 0) animated.moveTo(x, y) else animated.lineTo(x, y)
        }

        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 22f; color = Color.argb(100, 20, 40, 30) }
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 12f; color = Color.rgb(75, 210, 125); strokeCap = Paint.Cap.ROUND }
        val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.rgb(255, 193, 7) }
        canvas.drawPath(route, shadow)
        canvas.drawPath(animated, routePaint)

        val (startX, startY) = position(0)
        val (endX, endY) = position(points.lastIndex)
        canvas.drawCircle(startX, startY, 16f, marker)
        canvas.drawCircle(endX, endY, 16f, routePaint)

        if (photos.isNotEmpty()) {
            val photoIndex = visibleCount.coerceIn(0, points.lastIndex)
            val (x, y) = position(photoIndex)
            canvas.drawCircle(x, y, 24f, marker)
        }
        return bitmap
    }

    private fun argbToYuv420(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = pixels[j * width + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        return yuv
    }
}
