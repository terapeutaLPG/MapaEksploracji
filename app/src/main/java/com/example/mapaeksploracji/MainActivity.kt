
package com.example.mapaeksploracji
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Testowy zapis danych do Firestore
        val db = Firebase.firestore
        val testData = hashMapOf("name" to "Igor", "project" to "MapaEksploracji")

        db.collection("testCollection")
            .add(testData)
            .addOnSuccessListener { docRef ->
                println("✅ Dokument dodany: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                println("❌ Błąd dodawania dokumentu: $e")
            }

        setContent {
            // Twój UI tutaj
        }
    }
}
