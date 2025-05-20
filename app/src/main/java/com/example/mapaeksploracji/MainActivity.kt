//
//package com.example.mapaeksploracji
//
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.mapbox.maps.MapView
//import com.mapbox.maps.Style
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var mapView: MapView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        mapView = findViewById(R.id.mapView)
//
//        mapView.getMapboxMap().loadStyleUri(Style.DARK) // zmień na Style.LIGHT albo custom, jeśli chcesz
//    }
//
//    override fun onStart() {
//        super.onStart()
//        mapView.onStart()
//    }
//
//    override fun onStop() {
//        super.onStop()
//        mapView.onStop()
//    }
//
//    override fun onLowMemory() {
//        super.onLowMemory()
//        mapView.onLowMemory()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mapView.onDestroy()
//    }
//}
package com.example.mapaeksploracji

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.location2
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin


class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    // Launcher do żądania uprawnień
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

        mapView = findViewById(R.id.mapView)

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
