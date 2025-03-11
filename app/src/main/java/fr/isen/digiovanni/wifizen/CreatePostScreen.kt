package fr.isen.digiovanni.wifizen

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun CreatePostScreen(
    auth: FirebaseAuth,
    currentUserPseudo: String,
    currentUserProfileImageUrl: String,
    onPostCreated: () -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val database = Firebase.database(
        "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    val postsRef = database.getReference("posts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Text("Créer un Post", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Texte") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = imageUrl,
            onValueChange = {
                imageUrl = it
                error = ""
            },
            label = { Text("URL de l'image") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                fun isValidUrl(url: String): Boolean {
                    return try {
                        val uri = Uri.parse(url)
                        (uri.scheme == "http" || uri.scheme == "https") && uri.host != null
                    } catch (e: Exception) {
                        false
                    }
                }
                if (imageUrl.isNotBlank() && !isValidUrl(imageUrl)) {
                    error = "URL de l'image incorrecte."
                    return@Button
                }
                if (text.isNotBlank() || imageUrl.isNotBlank()) {
                    val newPost = mapOf(
                        "uid" to auth.currentUser?.uid.orEmpty(),
                        "pseudo" to currentUserPseudo,
                        "profileImageUrl" to currentUserProfileImageUrl,
                        "text" to text,
                        "imageUrl" to imageUrl,
                        "timestamp" to System.currentTimeMillis()
                    )
                    postsRef.push().setValue(newPost)
                        .addOnSuccessListener {
                            Log.d("CreatePostScreen", "Post créé")
                            onPostCreated()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CreatePostScreen", "Erreur création post", e)
                        }
                } else {
                    Log.d("CreatePostScreen", "Les champs sont vides.")
                }
            }) {
                Text("Poster")
            }
            Button(onClick = onCancel) {
                Text("Annuler")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}