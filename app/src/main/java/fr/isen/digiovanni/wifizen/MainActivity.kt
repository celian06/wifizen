package fr.isen.digiovanni.wifizen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

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
                var currentScreen by remember {
                    mutableStateOf(if (currentUser == null) "auth" else "feed")
                }

                // Écoute de l'état d'authentification
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        currentUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                // Si l'utilisateur se déconnecte, repasser sur l'écran d'authentification
                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        currentScreen = "auth"
                    }
                }

                // Charger les infos utilisateur (pseudo, photo de profil) depuis Firebase
                LaunchedEffect(currentUser) {
                    currentUser?.uid?.let { uid ->
                        val userRef = Firebase.database(
                            "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
                        ).getReference("users").child(uid)
                        userRef.get().addOnSuccessListener { snapshot ->
                            currentUserPseudo =
                                snapshot.child("pseudo").getValue(String::class.java) ?: ""
                            currentUserProfileImageUrl =
                                snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                        }
                    }
                }

                // Navigation simple par un when sur currentScreen
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