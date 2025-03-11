package fr.isen.digiovanni.wifizen

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// Data class pour un commentaire
data class Comment(
    var uid: String = "",
    var pseudo: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)

// Data class pour un post, incluant pseudo et l'URL de l'image de profil
data class Post(
    var uid: String = "",
    var pseudo: String = "",
    var profileImageUrl: String = "",
    var text: String = "",
    var imageUrl: String = "",
    var timestamp: Long = 0L,
    var likes: Map<String, Boolean> = emptyMap(),
    var comments: Map<String, Comment> = emptyMap()
)

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                // États globaux
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                var currentUserPseudo by remember { mutableStateOf("") }
                var currentUserProfileImageUrl by remember { mutableStateOf("") }
                // Déclaration de currentScreen dès le début
                var currentScreen by remember { mutableStateOf(if (currentUser == null) "auth" else "feed") }

                // Écoute de l'état d'authentification
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                // Si l'utilisateur se déconnecte, passer à l'écran d'authentification
                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        currentScreen = "auth"
                    }
                }

                // Chargement des infos utilisateur depuis Firebase
                LaunchedEffect(currentUser) {
                    currentUser?.uid?.let { uid ->
                        val userRef = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
                            .getReference("users").child(uid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                currentUserPseudo = snapshot.child("pseudo").getValue(String::class.java) ?: ""
                                currentUserProfileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MainActivity", "Erreur lors du chargement des infos utilisateur", error.toException())
                            }
                        })
                    }
                }

                // Navigation entre les écrans
                when (currentScreen) {
                    "auth" -> AuthScreen(
                        auth = auth,
                        onAuthSuccess = { currentScreen = "feed" }
                    )
                    "feed" -> FeedScreen(
                        auth = auth,
                        currentUserPseudo = currentUserPseudo,
                        onCreatePost = { currentScreen = "create" },
                        onProfileClick = { currentScreen = "profile" }
                    )
                    "create" -> CreatePostScreen(
                        auth = auth,
                        currentUserPseudo = currentUserPseudo,
                        currentUserProfileImageUrl = currentUserProfileImageUrl,
                        onPostCreated = { currentScreen = "feed" },
                        onCancel = { currentScreen = "feed" }
                    )
                    "profile" -> ProfileScreen(
                        currentUserUid = auth.currentUser?.uid ?: "",
                        currentPseudo = currentUserPseudo,
                        currentProfileImageUrl = currentUserProfileImageUrl,
                        onProfileUpdated = { newPseudo, newProfileImageUrl ->
                            currentUserPseudo = newPseudo
                            currentUserProfileImageUrl = newProfileImageUrl
                            currentScreen = "feed"
                        },
                        onBack = { currentScreen = "feed" }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth, onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pseudo by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf("login") } // "login" ou "signup"
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (authMode == "login") "Connexion" else "Inscription",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (authMode == "signup") {
            OutlinedTextField(
                value = pseudo,
                onValueChange = { pseudo = it },
                label = { Text("Pseudo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = profileImageUrl,
                onValueChange = { profileImageUrl = it },
                label = { Text("URL de la photo de profil") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                loading = true
                errorMessage = ""
                if (authMode == "login") {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                Log.d("Auth", "Connexion réussie")
                                onAuthSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Erreur lors de la connexion"
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                val userMap = mapOf(
                                    "uid" to uid,
                                    "pseudo" to pseudo,
                                    "profileImageUrl" to profileImageUrl
                                )
                                val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
                                database.getReference("users").child(uid).setValue(userMap)
                                    .addOnSuccessListener {
                                        Log.d("Auth", "Inscription et sauvegarde réussies")
                                        onAuthSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Auth", "Erreur lors de la sauvegarde", e)
                                    }
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Erreur lors de l'inscription"
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (authMode == "login") "Se connecter" else "S'inscrire")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                authMode = if (authMode == "login") "signup" else "login"
                errorMessage = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (authMode == "login") "Créer un compte" else "J'ai déjà un compte")
        }
        if (loading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun FeedScreen(
    auth: FirebaseAuth,
    currentUserPseudo: String,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit
) {
    val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // En-tête avec boutons affichant des emojis
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
                    var showDialog by remember { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Affichage de l'avatar et du pseudo
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (post.profileImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(post.profileImageUrl)
                                            .size(40)
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
                            Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
                            if (post.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(post.imageUrl)
                                        .size(600, 400)
                                        .build(),
                                    contentDescription = "Image du post",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (post.uid == auth.currentUser?.uid) {
                                Button(
                                    onClick = {
                                        postsRef.child(key).removeValue()
                                            .addOnSuccessListener { Log.d("FeedScreen", "Post supprimé") }
                                            .addOnFailureListener { e -> Log.e("FeedScreen", "Erreur de suppression", e) }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Supprimer", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Boutons like et commentaire
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
                                Button(onClick = { showDialog = true }) {
                                    Text("💬 Commenter")
                                }
                            }
                            if (post.comments.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Commentaires :", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    post.comments.values.forEach { comment ->
                                        Text("- ${comment.pseudo}: ${comment.text}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    if (showDialog) {
                        ShowCommentDialog(
                            postsRef = postsRef,
                            postId = key,
                            currentUserPseudo = post.pseudo,
                            onDismiss = { showDialog = false }
                        )
                    }
                }
            }
        } else {
            Text("Aucun post")
        }
    }
}

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
    val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
    val postsRef = database.getReference("posts")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
                    val post = Post(
                        uid = auth.currentUser?.uid ?: "",
                        pseudo = currentUserPseudo,
                        profileImageUrl = currentUserProfileImageUrl,
                        text = text,
                        imageUrl = imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    postsRef.push().setValue(post)
                        .addOnSuccessListener {
                            Log.d("CreatePostScreen", "Post créé")
                            onPostCreated()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CreatePostScreen", "Erreur lors de la création du post", e)
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
                    val comment = Comment(
                        uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        pseudo = currentUserPseudo,
                        text = commentText,
                        timestamp = System.currentTimeMillis()
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
    val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
    val userRef = database.getReference("users").child(currentUserUid)
    val postsRef = database.getReference("posts")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mon Profil", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (profileImageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profileImageUrl)
                    .size(100)
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
                val updates = mapOf("pseudo" to pseudo, "profileImageUrl" to profileImageUrl)
                userRef.updateChildren(updates).addOnSuccessListener {
                    Log.d("ProfileScreen", "Mise à jour du profil réussie")
                    // Mise à jour rétroactive des posts
                    postsRef.orderByChild("uid").equalTo(currentUserUid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                Log.d("ProfileScreen", "Nombre de posts à mettre à jour : ${snapshot.childrenCount}")
                                for (postSnapshot in snapshot.children) {
                                    postSnapshot.ref.updateChildren(
                                        mapOf("pseudo" to pseudo, "profileImageUrl" to profileImageUrl)
                                    )
                                }
                                updateMessage = "Profil mis à jour"
                                onProfileUpdated(pseudo, profileImageUrl)
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