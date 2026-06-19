package ni.edu.uam.uamlink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusLinkColorScheme = lightColorScheme(
    primary = VividGreen,
    secondary = PasteGreen,
    background = LightGreenBg,
    surface = Color.White, // Las tarjetas pueden ser blancas para resaltar sobre el fondo LightGreen
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorColor
)

@Composable
fun UAMlinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CampusLinkColorScheme,
        typography = Typography, // Importante: Aquí inyectamos la fuente Poppins
        content = content
    )
}