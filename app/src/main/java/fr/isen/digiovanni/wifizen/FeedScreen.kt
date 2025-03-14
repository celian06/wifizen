package fr.isen.digiovanni.wifizen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun FeedScreen(
    auth: FirebaseAuth,
    currentUserPseudo: String,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit
) {
    val database = Firebase.database(
        "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    val postsRef = database.getReference("posts")
    var posts by remember { mutableStateOf(listOf<Pair<String, Post>>()) }

    DisposableEffect(postsRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPosts = mutableListOf<Pair<String, Post>>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    val key = postSnapshot.key
                    if (post != null && key != null) {
                        newPosts.add(key to post)
                    }
                }
                newPosts.sortByDescending { it.second.timestamp }
                posts = newPosts
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("FeedScreen", "Erreur lors du chargement des posts", error.toException())
            }
        }
        postsRef.addValueEventListener(listener)
        onDispose { postsRef.removeEventListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fil d'actualité",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { auth.signOut() }) {
                Text("🔓")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onProfileClick) {
                Text("👤")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCreatePost) {
                Text("✏️")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (posts.isNotEmpty()) {
            LazyColumn {
                items(posts) { (key, post) ->
                    var showCommentDialog by remember { mutableStateOf(false) }
                    var showEditPostDialog by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (post.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(post.profileImageUrl)
                                            .build(),
                                        contentDescription = "Photo de profil",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (post.pseudo.isNotBlank()) post.pseudo else post.uid,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = post.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (post.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(post.imageUrl)
                                        .build(),
                                    contentDescription = "Image du post",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 2f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (post.uid == auth.currentUser?.uid) {
                                Row {
                                    Button(onClick = { showEditPostDialog = true }) {
                                        Text("Editer")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { postsRef.child(key).removeValue() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Supprimer", color = MaterialTheme.colorScheme.onError)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val currentUid = auth.currentUser?.uid ?: ""
                            val userLiked = post.likes.containsKey(currentUid)
                            val likeCount = post.likes.size

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(onClick = {
                                    val newLikes = post.likes.toMutableMap()
                                    if (userLiked) newLikes.remove(currentUid)
                                    else newLikes[currentUid] = true
                                    postsRef.child(key).child("likes").setValue(newLikes)
                                }) {
                                    Text("❤️ $likeCount")
                                }
                                Button(onClick = { showCommentDialog = true }) {
                                    Text("💬 Commenter")
                                }
                            }
                            if (post.comments.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Commentaires :", fontWeight = FontWeight.Bold)
                                    post.comments.forEach { (commentKey, comment) ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {

                                            Text(
                                                text = "- ${comment.pseudo}: ${comment.text}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                val currentUid = auth.currentUser?.uid ?: ""
                                                val userLiked = comment.likes.containsKey(currentUid)
                                                val userDisliked = comment.dislikes.containsKey(currentUid)

                                                val likeCount = comment.likes.size
                                                val dislikeCount = comment.dislikes.size

                                                // Bouton 👍
                                                Button(onClick = {
                                                    val newLikes = comment.likes.toMutableMap()
                                                    val newDislikes = comment.dislikes.toMutableMap()

                                                    if (userLiked) {
                                                        newLikes.remove(currentUid)  // Annuler le like
                                                    } else {
                                                        newLikes[currentUid] = true  // Ajouter le like
                                                        newDislikes.remove(currentUid)  // Retirer le dislike s'il existe
                                                    }

                                                    // Met à jour la base de données
                                                    postsRef.child(key)
                                                        .child("comments")
                                                        .child(commentKey)
                                                        .updateChildren(mapOf(
                                                            "likes" to newLikes,
                                                            "dislikes" to newDislikes
                                                        ))
                                                }) {
                                                    Text(if (userLiked) "👍 $likeCount" else "👍 $likeCount")
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // Bouton 👎
                                                Button(onClick = {
                                                    val newLikes = comment.likes.toMutableMap()
                                                    val newDislikes = comment.dislikes.toMutableMap()

                                                    if (userDisliked) {
                                                        newDislikes.remove(currentUid)  // Annuler le dislike
                                                    } else {
                                                        newDislikes[currentUid] = true  // Ajouter le dislike
                                                        newLikes.remove(currentUid)  // Retirer le like s'il existe
                                                    }

                                                    // Met à jour la base de données
                                                    postsRef.child(key)
                                                        .child("comments")
                                                        .child(commentKey)
                                                        .updateChildren(mapOf(
                                                            "likes" to newLikes,
                                                            "dislikes" to newDislikes
                                                        ))
                                                }) {
                                                    Text(if (userDisliked) "👎 $dislikeCount" else "👎 $dislikeCount")
                                                }
                                            }

                                            if (comment.uid == auth.currentUser?.uid) {
                                                var showEditCommentDialog by remember { mutableStateOf(false) }
                                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                                    Button(onClick = { showEditCommentDialog = true }) {
                                                        Text("Editer")
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            postsRef.child(key)
                                                                .child("comments")
                                                                .child(commentKey)
                                                                .removeValue()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.error
                                                        )
                                                    ) {
                                                        Text("Supprimer", color = MaterialTheme.colorScheme.onError)
                                                    }
                                                }
                                                if (showEditCommentDialog) {
                                                    EditCommentDialog(
                                                        postsRef = postsRef,
                                                        postId = key,
                                                        commentId = commentKey,
                                                        currentText = comment.text,
                                                        onDismiss = { showEditCommentDialog = false }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (showEditPostDialog) {
                        EditPostDialog(
                            postsRef = postsRef,
                            postId = key,
                            currentText = post.text,
                            currentImageUrl = post.imageUrl,
                            onDismiss = { showEditPostDialog = false }
                        )
                    }
                    if (showCommentDialog) {
                        ShowCommentDialog(
                            postsRef = postsRef,
                            postId = key,
                            currentUserPseudo = currentUserPseudo,
                            onDismiss = { showCommentDialog = false }
                        )
                    }
                }
            }
        } else {
            Text("Aucun post")
        }
    }
}