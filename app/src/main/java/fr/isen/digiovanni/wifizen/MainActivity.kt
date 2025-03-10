package fr.isen.digiovanni.wifizen

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// Modèle de données pour un post
data class Post(
    var uid: String = "",
    var text: String = "",
    var imageUrl: String = "",
    var videoUrl: String = "",
    var timestamp: Long = 0L
)

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialisation de Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            MaterialTheme {
                // Suivi de l'état de connexion de l'utilisateur
                var currentUser by remember { mutableStateOf(auth.currentUser) }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }
                if (currentUser == null) {
                    AuthScreen(auth = auth)
                } else {
                    // MainScreen gère la navigation entre le fil et l'écran de création de post
                    MainScreen(auth = auth)
                }
            }
        }
    }
}

@Composable
fun AuthScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf("login") } // "login" ou "signup"
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (authMode == "login") "Connexion" else "Inscription",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
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
                                errorMessage =
                                    task.exception?.localizedMessage ?: "Erreur lors de la connexion"
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            loading = false
                            if (task.isSuccessful) {
                                Log.d("Auth", "Inscription réussie")
                            } else {
                                errorMessage =
                                    task.exception?.localizedMessage ?: "Erreur lors de l'inscription"
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

// Gère la navigation entre le feed et l'écran de création de post
@Composable
fun MainScreen(auth: FirebaseAuth) {
    var currentScreen by remember { mutableStateOf("feed") } // "feed" ou "create"
    if (currentScreen == "feed") {
        FeedScreen(auth = auth, onCreatePost = { currentScreen = "create" })
    } else {
        CreatePostScreen(
            auth = auth,
            onPostCreated = { currentScreen = "feed" },
            onCancel = { currentScreen = "feed" }
        )
    }
}

@Composable
fun FeedScreen(auth: FirebaseAuth, onCreatePost: () -> Unit) {
    // Récupération de la référence à la base de données avec l'URL (note : ajouter le slash final si besoin)
    val database = Firebase.database("https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/")
    val postsRef = database.getReference("posts")
    var posts by remember { mutableStateOf(listOf<Post>()) }

    // Écoute en temps réel des posts
    DisposableEffect(postsRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPosts = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        newPosts.add(post)
                    }
                }
                newPosts.sortByDescending { it.timestamp }
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
        // En-tête avec titre, bouton Sign Out et bouton pour créer un nouveau post
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
        // Affichage des posts en temps réel
        if (posts.isNotEmpty()) {
            LazyColumn {
                items(posts) { post ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
                            if (post.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = "Image du post",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            if (post.videoUrl.isNotBlank()) {
                                // Affichage simplifié d'une vidéo via VideoView
                                val context = LocalContext.current
                                AndroidView(
                                    factory = {
                                        VideoView(context).apply {
                                            setVideoURI(Uri.parse(post.videoUrl))
                                            // On peut ajouter des contrôles ou démarrer automatiquement
                                            setOnPreparedListener { start() }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                            Text(
                                text = "Posté par : ${post.uid}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        } else {
            Text(text = "Aucun post")
        }
    }
}

@Composable
fun CreatePostScreen(auth: FirebaseAuth, onPostCreated: () -> Unit, onCancel: () -> Unit) {
    // Pour la création, on présente trois champs
    var text by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
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
            onValueChange = { imageUrl = it },
            label = { Text("URL de l'image") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            label = { Text("URL de la vidéo") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                // Création d'un nouveau post
                if (text.isNotBlank() || imageUrl.isNotBlank() || videoUrl.isNotBlank()) {
                    val post = Post(
                        uid = auth.currentUser?.uid ?: "",
                        text = text,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
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
                }
            }) {
                Text("Poster")
            }
            Button(onClick = onCancel) {
                Text("Annuler")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Astuce : Firebase Realtime Database est conçu pour des données légères.
        // Chaque opération d'écriture a une limite théorique de 256 Mo, mais il est recommandé
        // de ne pas dépasser quelques centaines de Ko par écritures.
    }
}