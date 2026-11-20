package com.mx.mountain

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.mx.mountain.databinding.FragmentGpxBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

class GpxFragment : Fragment() {
    private var _binding: FragmentGpxBinding? = null
    private val binding get() = _binding!!
    private var previewAnimator: ValueAnimator? = null
    private var previewStatsText = ""

    private val gpxPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { loadGpx(it) }
    }

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            binding.gpxStats.append("\nFotografías añadidas: ${uris.size}")
            binding.routeView.setPhotoUris(uris)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGpxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonImportGpx.setOnClickListener {
            gpxPicker.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*"))
        }
        binding.buttonImportPhotos.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }
        binding.buttonShareRoute.setOnClickListener { shareRoutePreview() }
        binding.buttonExportVideo.setOnClickListener { exportRouteVideo() }
        binding.buttonSatellite.setOnClickListener { binding.routeView.setSatellite() }
        binding.buttonTerrain.setOnClickListener { binding.routeView.setTerrain() }
        binding.buttonPreviewPlay.setOnClickListener { playPreview() }
        binding.buttonPreviewStop.setOnClickListener { stopPreview() }
    }

    private fun playPreview() {
        if (!binding.routeView.hasRoute()) {
            binding.gpxStats.text = "Importa primero un archivo GPX."
            return
        }

        previewAnimator?.cancel()
        binding.previewProgress.progress = 0
        binding.previewStatsOverlay.text = previewStatsText
        binding.previewStatsOverlay.visibility = View.GONE
        previewAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6000
            interpolator = LinearInterpolator()
            repeatCount = 0
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.previewStatsOverlay.text = previewStatsText
                    binding.previewStatsOverlay.visibility = View.VISIBLE
                }
            })
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                binding.previewProgress.progress = (progress * 100).toInt()
                binding.routeView.previewProgress(progress)
            }
            start()
        }
    }

    private fun stopPreview() {
        previewAnimator?.cancel()
        previewAnimator = null
        binding.previewProgress.progress = 0
        binding.previewStatsOverlay.visibility = View.GONE
        binding.routeView.previewProgress(0f)
    }

    private fun loadGpx(uri: Uri) {
        try {
            val points = mutableListOf<RoutePoint>()
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val parser = android.util.Xml.newPullParser()
                parser.setInput(input, null)
                var event = parser.eventType
                var latitude: Double? = null
                var longitude: Double? = null
                var elevation: Double? = null
                var time: Long? = null

                while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "trkpt", "rtept" -> {
                                latitude = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                                longitude = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            }
                            "ele" -> elevation = parser.nextText().toDoubleOrNull()
                            "time" -> time = parseGpxTime(parser.nextText())
                        }
                    } else if (event == org.xmlpull.v1.XmlPullParser.END_TAG &&
                        (parser.name == "trkpt" || parser.name == "rtept") &&
                        latitude != null && longitude != null
                    ) {
                        points.add(RoutePoint(latitude, longitude, elevation, time))
                    }
                    event = parser.next()
                }
            }

            if (points.size < 2) {
                binding.gpxStats.text = "No se encontraron suficientes puntos en el GPX."
                return
            }

            binding.routeView.setRoute(points)

            var distance = 0.0
            var gain = 0.0
            var maxElevation = points.first().elevation ?: Double.NaN
            var maxSpeed = 0.0

            for (i in 1 until points.size) {
                val previous = points[i - 1]
                val current = points[i]
                distance += haversine(previous.latitude, previous.longitude, current.latitude, current.longitude)

                if (previous.elevation != null && current.elevation != null) {
                    gain += max(0.0, current.elevation - previous.elevation)
                    maxElevation = max(maxElevation, current.elevation)
                }

                val elapsed = if (previous.time != null && current.time != null)
                    (current.time - previous.time) / 1000.0 else 0.0
                if (elapsed > 0) {
                    val speed = haversine(previous.latitude, previous.longitude, current.latitude, current.longitude) / elapsed * 3.6
                    maxSpeed = max(maxSpeed, speed)
                }
            }

            val maxElevationText = if (maxElevation.isNaN()) "No disponible" else "%.0f m".format(maxElevation)
            previewStatsText = "Distancia: %.2f km\nDesnivel positivo: %.0f m\nAltitud máxima: %s\nVelocidad máxima: %.1f km/h"
                .format(distance, gain, maxElevationText, maxSpeed)
            binding.gpxStats.text = "$previewStatsText\nPuntos: ${points.size}"
        } catch (exception: Exception) {
            binding.gpxStats.text = "No fue posible leer el GPX: ${exception.message}"
        }
    }

    private fun parseGpxTime(value: String): Long? {
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            )
            formats.firstNotNullOfOrNull { format ->
                format.timeZone = TimeZone.getTimeZone("UTC")
                runCatching { format.parse(value)?.time }.getOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun exportRouteVideo() {
        if (!binding.routeView.hasRoute()) {
            binding.gpxStats.text = "Importa primero un archivo GPX."
            return
        }

        binding.buttonExportVideo.isEnabled = false
        binding.gpxStats.text = "Generando video vertical 9:16…"
        binding.routeView.post {
        binding.routeView.exportVerticalVideo(requireContext(), { file, error ->
            requireActivity().runOnUiThread {
            binding.buttonExportVideo.isEnabled = true
            if (error != null || file == null) {
                binding.gpxStats.text = "No fue posible generar el video: ${error?.message ?: "error desconocido"}"
                return@runOnUiThread
            }

            binding.gpxStats.text = "Video generado correctamente: ruta_3d.mp4"
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir video de la ruta"))
            }
        }, previewStatsText)
        }
    }

    private fun shareRoutePreview() {
        val bitmap = binding.routeView.snapshot()
        val file = File(requireContext().cacheDir, "ruta_mountain.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir ruta"))
    }

    override fun onDestroyView() {
        previewAnimator?.cancel()
        previewAnimator = null
        super.onDestroyView()
        _binding = null
    }
}

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val time: Long?
)
