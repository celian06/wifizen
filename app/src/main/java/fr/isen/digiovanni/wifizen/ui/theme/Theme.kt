package fr.isen.digiovanni.wifizen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF2B2B2B),  // fond anthracite
    onBackground = Color.White,      // textes en blanc
    surface = Color(0xFF2B2B2B),       // même couleur pour les surfaces
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun WifizenTheme(
    darkTheme: Boolean = true,  // forcer le mode sombre
    dynamicColor: Boolean = false,  // désactiver la couleur dynamique pour conserver l'anthracite
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}