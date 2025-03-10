package fr.isen.digiovanni.wifizen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RealtimeDatabaseScreen()
        }
    }
}

@Composable
fun RealtimeDatabaseScreen() {
    val database = Firebase.database
    val myRef = database.getReference("message")

    var message by remember { mutableStateOf("") }

    // Lire les données de la base
    LaunchedEffect(Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                message = snapshot.getValue(String::class.java) ?: "Aucune donnée"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("RealtimeDB", "Erreur de lecture", error.toException())
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Valeur dans Firebase :", style = MaterialTheme.typography.titleLarge)
        Text(text = message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))

        Button(onClick = {
            myRef.setValue("Hello, Jetpack Compose!")
                .addOnSuccessListener { Log.d("RealtimeDB", "Donnée écrite avec succès !") }
                .addOnFailureListener { e -> Log.w("RealtimeDB", "Erreur d'écriture", e) }
        }) {
            Text("Écrire dans Firebase")
        }
    }
}
