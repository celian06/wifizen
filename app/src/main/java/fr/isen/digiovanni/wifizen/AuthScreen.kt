package fr.isen.digiovanni.wifizen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun AuthScreen(auth: FirebaseAuth, onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pseudo by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf("login") } // "login" ou "signup"
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Récupérer la densité et la hauteur de l'IME (clavier)
    val density = LocalDensity.current
    val imeHeightPx = WindowInsets.ime.getBottom(density)
    // Considère le clavier comme ouvert uniquement si sa hauteur dépasse 100 dp
    val isKeyboardOpen = imeHeightPx > with(density) { 100.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = if (isKeyboardOpen) Arrangement.Top else Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Si le clavier est ouvert, on ajoute un espace pour décaler le contenu
        if (isKeyboardOpen) {
            Spacer(modifier = Modifier.height(50.dp))
        }

        // Logo affiché au-dessus des champs de saisie
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.fillMaxWidth(0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))

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
                                onAuthSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage
                                    ?: "Erreur lors de la connexion"
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
                                val database = Firebase.database(
                                    "https://wifizen-b7b58-default-rtdb.europe-west1.firebasedatabase.app/"
                                )
                                database.getReference("users").child(uid).setValue(userMap)
                                    .addOnSuccessListener { onAuthSuccess() }
                                    .addOnFailureListener { e: Exception ->
                                        errorMessage = e.localizedMessage ?: "Erreur lors de la sauvegarde"
                                    }
                            } else {
                                errorMessage = task.exception?.localizedMessage
                                    ?: "Erreur lors de l'inscription"
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