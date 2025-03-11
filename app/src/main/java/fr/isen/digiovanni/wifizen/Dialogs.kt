package fr.isen.digiovanni.wifizen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun ShowCommentDialog(
    postsRef: DatabaseReference,
    postId: String,
    currentUserPseudo: String,
    onDismiss: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un commentaire") },
        text = {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("Votre commentaire") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                if (commentText.isNotBlank()) {
                    val comment = mapOf(
                        "uid" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                        "pseudo" to currentUserPseudo,
                        "text" to commentText,
                        "timestamp" to System.currentTimeMillis()
                    )
                    postsRef.child(postId).child("comments").push().setValue(comment)
                    onDismiss()
                }
            }) {
                Text("Poster")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditPostDialog(
    postsRef: DatabaseReference,
    postId: String,
    currentText: String,
    currentImageUrl: String,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentText) }
    var imageUrl by remember { mutableStateOf(currentImageUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le post") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Texte") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL de l'image") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updates = mapOf("text" to text, "imageUrl" to imageUrl)
                postsRef.child(postId).updateChildren(updates)
                    .addOnSuccessListener { onDismiss() }
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun EditCommentDialog(
    postsRef: DatabaseReference,
    postId: String,
    commentId: String,
    currentText: String,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le commentaire") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Commentaire") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val updates = mapOf("text" to text)
                postsRef.child(postId).child("comments").child(commentId).updateChildren(updates)
                    .addOnSuccessListener { onDismiss() }
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}