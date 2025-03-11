package fr.isen.digiovanni.wifizen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun ProfileScreen(
    currentUserUid: String,
    currentPseudo: String,
    currentProfileImageUrl: String,
    onProfileUpdated: (newPseudo: String, newProfileImageUrl: String) -> Unit,
    onBack: () -> Unit
) {
    var pseudo by remember { mutableStateOf(currentPseudo) }
    var profileImageUrl by remember { mutableStateOf(currentProfileImageUrl) }
    var updateMessage by remember { mutableStateOf("") }

    val database = Firebase.database(
        "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    val userRef = database.getReference("users").child(currentUserUid)
    val postsRef = database.getReference("posts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text("Mon Profil", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .build(),
                contentDescription = "Photo de profil",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pseudo,
            onValueChange = { pseudo = it },
            label = { Text("Pseudo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = profileImageUrl,
            onValueChange = { profileImageUrl = it },
            label = { Text("URL de la photo de profil") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (updateMessage.isNotBlank()) {
            Text(updateMessage, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                val updates = mapOf(
                    "pseudo" to pseudo,
                    "profileImageUrl" to profileImageUrl
                )
                userRef.updateChildren(updates).addOnSuccessListener {
                    // Mise à jour des posts de l'utilisateur
                    postsRef.orderByChild("uid").equalTo(currentUserUid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (postSnapshot in snapshot.children) {
                                    postSnapshot.ref.updateChildren(
                                        mapOf(
                                            "pseudo" to pseudo,
                                            "profileImageUrl" to profileImageUrl
                                        )
                                    )
                                }
                                // Mise à jour des commentaires
                                postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        for (postSnapshot in snapshot.children) {
                                            val commentsSnapshot = postSnapshot.child("comments")
                                            for (commentSnapshot in commentsSnapshot.children) {
                                                val commentUid = commentSnapshot.child("uid").getValue(String::class.java)
                                                if (commentUid == currentUserUid) {
                                                    commentSnapshot.ref.updateChildren(
                                                        mapOf("pseudo" to pseudo)
                                                    )
                                                }
                                            }
                                        }
                                        updateMessage = "Profil mis à jour"
                                        onProfileUpdated(pseudo, profileImageUrl)
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        updateMessage = "Erreur lors de la mise à jour des commentaires"
                                    }
                                })
                            }
                            override fun onCancelled(error: DatabaseError) {
                                updateMessage = "Erreur lors de la mise à jour des posts"
                            }
                        })
                }.addOnFailureListener {
                    updateMessage = "Erreur lors de la mise à jour"
                }
            }) {
                Text("Sauvegarder")
            }
            Button(onClick = onBack) {
                Text("Retour")
            }
        }
    }
}