package com.mx.mountain

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.widget.FrameLayout
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.TileSet
import org.json.JSONObject

class SatelliteRouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val mapView: MapView
    private var points = listOf<RoutePoint>()
    private var visiblePolyline: Any? = null
    private var progressMarker: Any? = null
    var onMapReady: (() -> Unit)? = null

    init {
        MapLibre.getInstance(context)
        mapView = MapView(context)
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        mapView.onCreate(null)
    }

    fun setRoute(route: List<RoutePoint>) {
        points = route
        mapView.getMapAsync { map ->
            val styleJson = """
                {
                  "version": 8,
                  "sources": {
                    "esri-satellite": {
                      "type": "raster",
                      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
                      "tileSize": 256,
                      "attribution": "© Esri, Maxar, Earthstar Geographics"
                    }
                  },
                  "layers": [
                    {"id": "satellite", "type": "raster", "source": "esri-satellite", "paint": {"raster-saturation": -0.1, "raster-brightness-max": 0.95}}
                  ]
                }
            """.trimIndent()
            map.setStyle(Style.Builder().fromJson(styleJson)) { style ->
                val latLngs = points.map { LatLng(it.latitude, it.longitude) }
                if (latLngs.size >= 2) {
                    map.clear()
                    map.addPolyline(PolylineOptions().addAll(latLngs).color(0xFF4BD27D.toInt()).width(10f))
                    map.addMarker(MarkerOptions().position(latLngs.first()).title("Inicio"))
                    map.addMarker(MarkerOptions().position(latLngs.last()).title("Fin"))

                    val bounds = LatLngBounds.Builder().includes(latLngs).build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                    val camera = map.cameraPosition
                    map.animateCamera(CameraUpdateFactory.tiltTo(camera.tilt.coerceAtLeast(35.0)))
                    map.uiSettings.isTiltGesturesEnabled = true
                    map.uiSettings.isRotateGesturesEnabled = true
                    map.cameraPosition = map.cameraPosition
                }
                onMapReady?.invoke()
            }
        }
    }

    fun previewProgress(progress: Float) {
        if (points.size < 2) return
        if (progress <= 0f) {
            mapView.getMapAsync { map ->
                progressMarker?.let {
                    if (it is org.maplibre.android.annotations.Marker) map.removeMarker(it)
                }
                progressMarker = null
            }
        }
        val count = (points.size * progress).toInt().coerceIn(1, points.size)
        val visiblePoints = points.take(count).map { LatLng(it.latitude, it.longitude) }

        mapView.getMapAsync { map ->
            visiblePolyline?.let {
                if (it is org.maplibre.android.annotations.Polyline) map.removePolyline(it)
            }
            visiblePolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(visiblePoints)
                    .color(0xFFFFC107.toInt())
                    .width(12f)
            )

            val current = visiblePoints.last()
            progressMarker?.let {
                if (it is org.maplibre.android.annotations.Marker) map.removeMarker(it)
            }
            progressMarker = map.addMarker(
                MarkerOptions()
                    .position(current)
                    .title("● Recorriendo")
                    .snippet("Punto ${count} de ${points.size}" + (points[count - 1].elevation?.let { " · %.0f m".format(it) } ?: ""))
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15.0))
        }
    }

    fun hasRoute(): Boolean = points.size >= 2

    fun setSatellite() {
        setRoute(points)
    }

    fun setPhotoUris(uris: List<Uri>) {
        // Las fotografías se asocian por posición relativa al recorrer el GPX.
    }

    fun setTerrain() {
        mapView.getMapAsync { map ->
            map.setStyle("https://tiles.stadiamaps.com/styles/outdoors.json")
        }
    }

    fun exportVerticalVideo(context: Context, callback: (java.io.File?, Exception?) -> Unit, statisticsText: String = "") {
        mapView.getMapAsync { map ->
            mapView.postDelayed({
                val preview = snapshot()
                RouteVideoEncoder.encodeWithMapBackground(
                    context,
                    java.io.File(context.cacheDir, "ruta_3d.mp4"),
                    preview,
                    points,
                    callback,
                    statisticsText
                )
            }, 1500)
        }
    }

    fun snapshot(): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(540),
            height.coerceAtLeast(960),
            Bitmap.Config.ARGB_8888
        )
        val location = IntArray(2)
        mapView.getLocationOnScreen(location)
        val rect = android.graphics.Rect(location[0], location[1], location[0] + mapView.width, location[1] + mapView.height)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                val listener = android.view.PixelCopy.OnPixelCopyFinishedListener { }
                android.view.PixelCopy.request(window, rect, bitmap, listener, Handler(Looper.getMainLooper()))
            }
        }
        return bitmap
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mapView.onStart()
        mapView.onResume()
    }

    override fun onDetachedFromWindow() {
        mapView.onPause()
        mapView.onStop()
        mapView.onDestroy()
        super.onDetachedFromWindow()
    }
}
