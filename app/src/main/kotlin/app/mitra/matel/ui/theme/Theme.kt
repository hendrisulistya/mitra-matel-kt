package app.mitra.matel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    // You can customize other colors here if needed
)

@Composable
fun MitraMatelTheme(
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false by default to use our violet theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> {
            // Using our explicitly defined violet color scheme
            LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}