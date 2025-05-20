package com.example.mapaeksploracji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.imageSource
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val db = Firebase.firestore
    private var lastSectorId: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) enableUserLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        mapView = findViewById(R.id.mapView)

        // 🔍 Ustaw startowy zoom i umożliw bardzo duże przybliżenie
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(22.0)
                .build()
        )

        // ✋ Włącz wszystkie gesty
        mapView.gestures.apply {
            pinchToZoomEnabled = true
            doubleTapToZoomInEnabled = true
            quickZoomEnabled = true
            scrollEnabled = true
            rotateEnabled = true
            pitchEnabled = true
        }

        findViewById<Button>(R.id.btnUp).setOnClickListener { moveCamera(0.001, 0.0) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { moveCamera(-0.001, 0.0) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { moveCamera(0.0, -0.001) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { moveCamera(0.0, 0.001) }

        mapView.getMapboxMap().loadStyleUri(Style.LIGHT) { style ->
            checkLocationPermission()
            addSatelliteOverlayForVisitedAreas()

        }
    }

    private fun moveCamera(dLat: Double, dLng: Double) {
        val currentCenter = mapView.getMapboxMap().cameraState.center
        val newLat = currentCenter.latitude() + dLat
        val newLng = currentCenter.longitude() + dLng
        val newPoint = Point.fromLngLat(newLng, newLat)

        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(newPoint)
                .build()
        )

        val roundedLat = (newLat * 1000).toInt() / 1000.0
        val roundedLng = (newLng * 1000).toInt() / 1000.0
        val sectorId = "$roundedLat:$roundedLng"
        val data = hashMapOf("lat" to roundedLat, "lng" to roundedLng)

        db.collection("visitedAreas").document(sectorId).set(data)
    }

    private fun addSatelliteOverlayForVisitedAreas() {
        db.collection("visitedAreas").get()
            .addOnSuccessListener { documents ->
                mapView.getMapboxMap().getStyle { style ->
                    for (doc in documents) {
                        val lat = doc.getDouble("lat") ?: continue
                        val lng = doc.getDouble("lng") ?: continue

                        val roundedLat = (lat * 500).toInt() / 500.0
                        val roundedLng = (lng * 500).toInt() / 500.0


                        val id = "tile_${roundedLat}_${roundedLng}"
                        val imageUrl = "https://api.mapbox.com/styles/v1/mapbox/satellite-v9/static/$roundedLng,$roundedLat,19/256x256?access_token=pk.eyJ1Ijoic2ltb3hrc3kiLCJhIjoiY21hd3hwcnEwMGduZDJqc2U5N3QzczJlbiJ9.wBoenJhdDAtikyW9g3q8mw"

                        val delta = 0.00075 // rozmiar ~150m

                        val offset = 0.000675  // ok. 75 m w każdą stronę
                        val bounds = listOf(
                            listOf(lng - offset, lat - offset),
                            listOf(lng + offset, lat - offset),
                            listOf(lng + offset, lat + offset),
                            listOf(lng - offset, lat + offset)
                        )



                        if (!style.styleSourceExists(id)) {
                            val source = imageSource(id) {
                                coordinates(bounds)
                                url(imageUrl)
                            }
                            val layer = rasterLayer("${id}_layer", id) {
                                rasterOpacity(1.0)
                                visibility(Visibility.VISIBLE)
                            }
                            style.addSource(source)
                            style.addLayerAbove(layer, "waterway-label")
                            Log.d("OverlayDebug", "Dodano kafel $id -> $imageUrl")
                        }
                    }
                }
            }
    }






    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableUserLocation() {
        val locationComponent = mapView.location

        locationComponent.updateSettings {
            enabled = true
            pulsingEnabled = true
            locationPuck = LocationPuck2D(
                bearingImage = AppCompatResources.getDrawable(
                    this@MainActivity, com.mapbox.maps.R.drawable.mapbox_user_icon
                )
            )
        }

        var userMovedMap = false
        var lastMoveTime = System.currentTimeMillis()

        mapView.gestures.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                userMovedMap = true
                lastMoveTime = System.currentTimeMillis()
            }

            override fun onMove(detector: MoveGestureDetector): Boolean {
                lastMoveTime = System.currentTimeMillis()
                return false
            }

            override fun onMoveEnd(detector: MoveGestureDetector) {
                lastMoveTime = System.currentTimeMillis()
                handler.postDelayed({
                    userMovedMap = false
                }, 3000)
            }
        })

        locationComponent.addOnIndicatorPositionChangedListener(object :
            OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                if (!userMovedMap || System.currentTimeMillis() - lastMoveTime > 3000) {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(14.0)
                            .build()
                    )
                    userMovedMap = false
                }

                val lat = point.latitude()
                val lng = point.longitude()
                val roundedLat = (lat * 1000).toInt() / 1000.0
                val roundedLng = (lng * 1000).toInt() / 1000.0
                val sectorId = "$roundedLat:$roundedLng"

                if (sectorId != lastSectorId) {
                    lastSectorId = sectorId
                    val data = hashMapOf("lat" to roundedLat, "lng" to roundedLng)
                    db.collection("visitedAreas").document(sectorId).set(data)
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
