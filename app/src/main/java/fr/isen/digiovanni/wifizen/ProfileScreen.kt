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
import androidx.compose.ui.Alignment

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

    // Variables pour les statistiques
    var postCount by remember { mutableStateOf(0) }
    var commentCount by remember { mutableStateOf(0) }
    var likeCount by remember { mutableStateOf(0) }

    val database = Firebase.database(
        "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    val userRef = database.getReference("users").child(currentUserUid)
    val postsRef = database.getReference("posts")

    // Récupérer les statistiques de l'utilisateur
    LaunchedEffect(currentUserUid) {
        postsRef.orderByChild("uid").equalTo(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var postCounter = 0
                    var commentCounter = 0
                    var likeCounter = 0
                    for (postSnapshot in snapshot.children) {
                        postCounter++
                        val commentsSnapshot = postSnapshot.child("comments")
                        commentCounter += commentsSnapshot.childrenCount.toInt()
                        likeCounter += postSnapshot.child("likes").childrenCount.toInt()
                    }
                    postCount = postCounter
                    commentCount = commentCounter
                    likeCount = likeCounter
                }
                override fun onCancelled(error: DatabaseError) {
                    // Gérer l'erreur si nécessaire
                }
            })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text("Mon Profil", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Box pour centrer la photo de profil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
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
        }

        // Barre de statistiques sous la photo de profil
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatItem(label = "Posts", count = postCount, modifier = Modifier.weight(1f))
            StatItem(label = "Commentaires", count = commentCount, modifier = Modifier.weight(1f))
            StatItem(label = "Likes", count = likeCount, modifier = Modifier.weight(1f))
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

@Composable
fun StatItem(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}