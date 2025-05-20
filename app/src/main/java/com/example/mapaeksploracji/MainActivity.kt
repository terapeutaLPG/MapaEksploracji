package com.example.mapaeksploracji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.sources.TileSet
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.RasterSource

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val db = Firebase.firestore
    private var lastSectorId: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableUserLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        mapView = findViewById(R.id.mapView)
        findViewById<Button>(R.id.btnUp).setOnClickListener { moveCamera(0.001, 0.0) }
        findViewById<Button>(R.id.btnDown).setOnClickListener { moveCamera(-0.001, 0.0) }
        findViewById<Button>(R.id.btnLeft).setOnClickListener { moveCamera(0.0, -0.001) }
        findViewById<Button>(R.id.btnRight).setOnClickListener { moveCamera(0.0, 0.001) }


        mapView.getMapboxMap().loadStyleUri(Style.LIGHT) { style ->
            checkLocationPermission()
            addSatelliteOverlayForVisitedAreas(style)
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

        // Możesz też wywołać zapis do Firebase
        val roundedLat = (newLat * 1000).toInt() / 1000.0
        val roundedLng = (newLng * 1000).toInt() / 1000.0
        val sectorId = "$roundedLat:$roundedLng"
        val data = hashMapOf("lat" to roundedLat, "lng" to roundedLng)

        Firebase.firestore.collection("visitedAreas").document(sectorId).set(data)
    }

    private fun addSatelliteOverlayForVisitedAreas(style: Style) {
        val db = Firebase.firestore
        db.collection("visitedAreas").get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val lat = doc.getDouble("lat")
                    val lng = doc.getDouble("lng")
                    if (lat != null && lng != null) {
                        val tileId = "${lat}_${lng}"

                        style.addSource(
                            RasterSource.Builder(tileId)
                                .tileSet(
                                    TileSet.Builder("tileset", listOf(
                                        "https://api.mapbox.com/styles/v1/mapbox/satellite-v9/tiles/256/{z}/{x}/{y}?access_token=YOUR_MAPBOX_TOKEN"
                                    )).build()
                                )
                                .tileSize(256)
                                .build()
                        )

                        style.addLayerBelow(
                            RasterLayer("${tileId}_layer", tileId),
                            "water" // lub inna warstwa referencyjna
                        )
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

        mapView.gestures.addOnMoveListener(object : com.mapbox.maps.plugin.gestures.OnMoveListener {
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
                // automatyczne przybliżenie tylko jeśli użytkownik nie przesuwa mapy
                if (!userMovedMap || System.currentTimeMillis() - lastMoveTime > 3000) {
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(14.0)
                            .build()
                    )
                    userMovedMap = false
                }

                // zapis sektora
                val roundedLat = (point.latitude() * 1000).toInt() / 1000.0
                val roundedLng = (point.longitude() * 1000).toInt() / 1000.0
                val sectorId = "$roundedLat:$roundedLng"
                if (sectorId != lastSectorId) {
                    lastSectorId = sectorId
                    val data = hashMapOf("lat" to roundedLat, "lng" to roundedLng)
                    db.collection("visitedAreas").document(sectorId).set(data)

                    val annotationApi = mapView.annotations
                    val circleManager = annotationApi.createCircleAnnotationManager()
                    val circle = CircleAnnotationOptions()
                        .withPoint(Point.fromLngLat(roundedLng, roundedLat))
                        .withCircleRadius(12.0)
                        .withCircleColor("#0096FF")
                        .withCircleStrokeWidth(0.0)
                    circleManager.create(circle)
                }
            }
        })
    }

    private fun loadVisitedSectors() {
        val annotationApi = mapView.annotations
        val circleManager = annotationApi.createCircleAnnotationManager()

        db.collection("visitedAreas").get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val lat = doc.getDouble("lat")
                    val lng = doc.getDouble("lng")
                    if (lat != null && lng != null) {
                        val options = CircleAnnotationOptions()
                            .withPoint(Point.fromLngLat(lng, lat))
                            .withCircleRadius(12.0)
                            .withCircleColor("#0096FF")
                            .withCircleStrokeWidth(0.0)

                        circleManager.create(options)
                    }
                }
            }
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
