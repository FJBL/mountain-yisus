package com.mx.mountain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.graphics.Bitmap
import android.net.Uri
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import java.io.File

class RouteAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points = listOf<RoutePoint>()
    private var progress = 0f
    private var photos = listOf<Uri>()
    private var mapMode = RouteMapMode.SATELLITE

    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.rgb(35, 126, 74)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.argb(70, 20, 70, 40)
    }

    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(232, 241, 232)
    }

    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(255, 193, 7)
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 5000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { progress = it.animatedValue as Float; invalidate() }
    }

    fun setRoute(route: List<RoutePoint>) {
        points = route
        progress = 0f
        animator.start()
    }

    fun setMapMode(mode: RouteMapMode) {
        mapMode = mode
        invalidate()
    }

    fun setPhotoUris(uris: List<Uri>) {
        photos = uris
        invalidate()
    }

    fun hasRoute(): Boolean = points.size >= 2

    fun snapshot(): Bitmap {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }

    fun exportVerticalVideo(context: Context, callback: (File?, Exception?) -> Unit) {
        if (points.size < 2) {
            callback(null, IllegalStateException("No hay una ruta cargada"))
            return
        }

        try {
            val file = File(context.cacheDir, "ruta_3d.mp4")
            if (file.exists()) file.delete()
            RouteVideoEncoder.encode(context, file, points, photos, mapMode, { encodedFile, error ->
                post { callback(encodedFile, error) }
            })
        } catch (error: Exception) {
            callback(null, error)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val backgroundPaint = if (mapMode == RouteMapMode.SATELLITE)
            Paint().apply { color = Color.rgb(42, 54, 48) }
        else terrainPaint
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (mapMode == RouteMapMode.SATELLITE) {
            val gridPaint = Paint().apply {
                color = Color.argb(55, 180, 210, 185)
                strokeWidth = 2f
            }
            var y = 0f
            while (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                y += 48f
            }
            var x = 0f
            while (x < width) {
                canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
                x += 48f
            }
        }

        if (points.size < 2) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 42f }
            canvas.drawText("Vista animada de la ruta", 40f, height / 2f, paint)
            return
        }

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        val latRange = (maxLat - minLat).coerceAtLeast(0.000001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.000001)
        val padding = 40f

        fun position(index: Int): Pair<Float, Float> {
            val point = points[index]
            val x = padding + (((point.longitude - minLon) / lonRange) * (width - padding * 2)).toFloat()
            val y = (height - padding) - (((point.latitude - minLat) / latRange) * (height - padding * 2)).toFloat()
            val elevationFactor = point.elevation?.let { (it / 10000.0).toFloat() } ?: 0f
            return x to y - elevationFactor * 20f
        }

        val fullPath = Path()
        val animatedPath = Path()
        val visibleCount = (points.size * progress).toInt().coerceAtLeast(1)

        repeat(points.size) { i ->
            val (x, y) = position(i)
            if (i == 0) fullPath.moveTo(x, y) else fullPath.lineTo(x, y)
        }

        repeat(visibleCount) { i ->
            val (x, y) = position(i)
            if (i == 0) animatedPath.moveTo(x, y) else animatedPath.lineTo(x, y)
        }

        canvas.drawPath(fullPath, shadowPaint)
        canvas.drawPath(fullPath, Paint(routePaint).apply { color = Color.argb(90, 35, 126, 74) })
        canvas.drawPath(animatedPath, routePaint)

        val (startX, startY) = position(0)
        val (endX, endY) = position(points.lastIndex)
        canvas.drawCircle(startX, startY, 12f, photoPaint)
        canvas.drawCircle(endX, endY, 12f, routePaint)

        if (photos.isNotEmpty()) {
            val photoIndex = visibleCount.coerceIn(0, points.lastIndex)
            val (photoX, photoY) = position(photoIndex)
            canvas.drawCircle(photoX, photoY, 18f, photoPaint)
            canvas.drawCircle(photoX, photoY, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
