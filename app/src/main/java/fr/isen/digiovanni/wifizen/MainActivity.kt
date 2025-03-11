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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

// Data class pour un commentaire (incluant le pseudo du commentateur)
data class Comment(
    var uid: String = "",
    var pseudo: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)

// Data class pour un post, incluant le pseudo, les likes (sous forme d'une Map de uid) et les commentaires
data class Post(
    var uid: String = "",
    var pseudo: String = "",
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
        // Initialisation de Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                // Suivi de l'état de connexion
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                // Stockage du pseudo de l'utilisateur connecté
                var currentUserPseudo by remember { mutableStateOf("") }

                // Listener d'authentification
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                // Chargement du pseudo depuis Firebase si l'utilisateur est connecté
                LaunchedEffect(currentUser) {
                    currentUser?.uid?.let { uid ->
                        val userRef = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
                            .getReference("users").child(uid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                currentUserPseudo = snapshot.child("pseudo").getValue(String::class.java) ?: ""
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("MainActivity", "Erreur lors du chargement du pseudo", error.toException())
                            }
                        })
                    }
                }

                if (currentUser == null) {
                    AuthScreen(auth = auth)
                } else {
                    MainScreen(auth = auth, currentUserPseudo = currentUserPseudo)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pseudo by remember { mutableStateOf("") } // Champ pour le pseudo lors de l'inscription
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
            visualTransformation = PasswordVisualTransformation(), // Masque le mot de passe
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
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Erreur lors de la connexion"
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                // Sauvegarde du pseudo dans la base de données
                                val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                                val userMap = mapOf("uid" to uid, "pseudo" to pseudo)
                                val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
                                database.getReference("users").child(uid).setValue(userMap)
                                    .addOnSuccessListener {
                                        Log.d("Auth", "Inscription et sauvegarde du pseudo réussies")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Auth", "Erreur lors de la sauvegarde du pseudo", e)
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
fun MainScreen(auth: FirebaseAuth, currentUserPseudo: String) {
    var currentScreen by remember { mutableStateOf("feed") } // "feed" ou "create"
    if (currentScreen == "feed") {
        // On transmet également le pseudo de l'utilisateur courant à FeedScreen
        FeedScreen(auth = auth, currentUserPseudo = currentUserPseudo, onCreatePost = { currentScreen = "create" })
    } else {
        CreatePostScreen(
            auth = auth,
            currentUserPseudo = currentUserPseudo,
            onPostCreated = { currentScreen = "feed" },
            onCancel = { currentScreen = "feed" }
        )
    }
}

@Composable
fun FeedScreen(auth: FirebaseAuth, currentUserPseudo: String, onCreatePost: () -> Unit) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Fil d'actualité", style = MaterialTheme.typography.titleLarge)
            Row {
                Button(onClick = { auth.signOut() }) {
                    Text(text = "Sign Out")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onCreatePost) {
                    Text(text = "Nouveau Post")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (posts.isNotEmpty()) {
            LazyColumn {
                items(posts) { (key, post) ->
                    var showDialog by remember { mutableStateOf(false) }
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
                            if (post.imageUrl.isNotBlank()) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(post.imageUrl)
                                        .size(600, 400)
                                        .build(),
                                    contentDescription = "Image du post",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            Text(
                                text = "Posté par : ${if (post.pseudo.isNotBlank()) post.pseudo else post.uid}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp
                            )
                            // Bouton de suppression (affiché si le post appartient à l'utilisateur courant)
                            if (post.uid == auth.currentUser?.uid) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        postsRef.child(key).removeValue()
                                            .addOnSuccessListener {
                                                Log.d("FeedScreen", "Post supprimé avec succès")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("FeedScreen", "Erreur lors de la suppression du post", e)
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(text = "Supprimer", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Gestion du like : on stocke les likes dans une Map<String, Boolean>
                            val currentUid = auth.currentUser?.uid ?: ""
                            val userLiked = post.likes.containsKey(currentUid)
                            val likeCount = post.likes.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(onClick = {
                                    val newLikes = post.likes.toMutableMap()
                                    if (userLiked) {
                                        newLikes.remove(currentUid)
                                    } else {
                                        newLikes[currentUid] = true
                                    }
                                    postsRef.child(key).child("likes").setValue(newLikes)
                                }) {
                                    Text("❤️ $likeCount")
                                }
                                Button(onClick = { showDialog = true }) {
                                    Text("💬 Commenter")
                                }
                            }
                            // Affichage des commentaires avec pseudo
                            if (post.comments.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Commentaires :", fontWeight = FontWeight.Bold)
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
                            currentUserPseudo = currentUserPseudo,
                            onDismiss = { showDialog = false }
                        )
                    }
                }
            }
        } else {
            Text(text = "Aucun post")
        }
    }
}

@Composable
fun CreatePostScreen(
    auth: FirebaseAuth,
    currentUserPseudo: String,
    onPostCreated: () -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
    val postsRef = database.getReference("posts")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Créer un Post", style = MaterialTheme.typography.titleLarge)
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
            Text(text = error, color = MaterialTheme.colorScheme.error)
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
                        text = text,
                        imageUrl = imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                    postsRef.push().setValue(post)
                        .addOnSuccessListener {
                            Log.d("CreatePostScreen", "Post créé avec succès")
                            onPostCreated()
                        }
                        .addOnFailureListener { e ->
                            Log.e("CreatePostScreen", "Erreur lors de la création du post", e)
                        }
                } else {
                    Log.d("CreatePostScreen", "Les champs sont vides, aucun post n'est créé.")
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
        onDismissRequest = { onDismiss() },
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
            Button(onClick = { onDismiss() }) {
                Text("Annuler")
            }
        }
    )
}