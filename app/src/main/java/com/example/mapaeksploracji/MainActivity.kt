package com.example.mapaeksploracji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

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

        // Inicjalizacja Firebase
        FirebaseApp.initializeApp(this)

        mapView = findViewById(R.id.mapView)

        // Załaduj styl mapy i sprawdź uprawnienia do lokalizacji
        mapView.getMapboxMap().loadStyleUri(Style.DARK) {
            checkLocationPermission()
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
        val locationComponentPlugin = mapView.location

        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = true
            locationPuck = LocationPuck2D()
        }

        // Nasłuchiwanie lokalizacji
        val listener = object : OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                // Przybliż do pozycji
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build()
                )

                // Zapisz sektor do Firebase
                val roundedLat = (point.latitude() * 1000).toInt() / 1000.0
                val roundedLng = (point.longitude() * 1000).toInt() / 1000.0
                val sectorId = "$roundedLat:$roundedLng"

                val data = hashMapOf("lat" to roundedLat, "lng" to roundedLng)

                Firebase.firestore.collection("visitedAreas").document(sectorId)
                    .set(data)
                    .addOnSuccessListener {
                        println("✅ Zapisano sektor: $sectorId")
                    }
                    .addOnFailureListener { e ->
                        println("❌ Błąd zapisu sektora: $e")
                    }

                locationComponentPlugin.removeOnIndicatorPositionChangedListener(this)
            }
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener(listener)
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
