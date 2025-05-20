//
//package com.example.mapaeksploracji
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import com.google.firebase.firestore.ktx.firestore
//import com.google.firebase.ktx.Firebase
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Testowy zapis danych do Firestore
//        val db = Firebase.firestore
//        val testData = hashMapOf("name" to "Igor", "project" to "MapaEksploracji")
//
//        db.collection("testCollection")
//            .add(testData)
//            .addOnSuccessListener { docRef ->
//                println("✅ Dokument dodany: ${docRef.id}")
//            }
//            .addOnFailureListener { e ->
//                println("❌ Błąd dodawania dokumentu: $e")
//            }
//
//        setContent {
//            // Twój UI tutaj
//        }
//    }
//}
package com.example.mapaeksploracji

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)

        mapView.getMapboxMap().loadStyleUri(Style.DARK) // zmień na Style.LIGHT albo custom, jeśli chcesz
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
