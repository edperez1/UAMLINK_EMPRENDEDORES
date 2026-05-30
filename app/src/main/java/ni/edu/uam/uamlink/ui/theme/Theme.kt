package ni.edu.uam.uamlink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = UAMBackground,
    surface = UAMSurface,
    primary = UAMGreen,
    onPrimary = UAMTextPrimary,
    onBackground = UAMTextPrimary,
    onSurface = UAMTextPrimary,
    error = UAMError
)

@Composable
fun UAMlinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}