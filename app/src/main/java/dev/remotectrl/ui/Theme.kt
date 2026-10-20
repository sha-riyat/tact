package dev.remotectrl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Pure black screen, #1C1C1D keys, white marks: the look of a hardware remote. */
object Palette {
    val Black = Color(0xFF000000)
    val Key = Color(0xFF1C1C1D)
    val KeyPressed = Color(0xFF2C2C2E)
    val KeyHeld = Color(0xFF3A3A3C)
    val Mark = Color(0xFFFFFFFF)
    val Muted = Color(0xFF8E8E93)
    val Online = Color(0xFF30D158)
    val Offline = Color(0xFFFF453A)
}

private val Colors = darkColorScheme(
    primary = Palette.Mark,
    onPrimary = Palette.Black,
    background = Palette.Black,
    onBackground = Palette.Mark,
    surface = Palette.Key,
    onSurface = Palette.Mark,
    surfaceVariant = Palette.Key,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Key,
    surfaceContainerHigh = Palette.Key,
    error = Palette.Offline,
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
