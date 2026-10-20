package dev.remotectrl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One material family: a graphite plate with matte caps, a single warm accent.
 * "Lift" is the slightly lighter tone the top of a cap fades from.
 */
object Palette {
    val PlateTop = Color(0xFF3A4146)
    val PlateBottom = Color(0xFF2D3236)
    val Ink = Color(0xFF0D0F10)
    val Well = Color(0xFF15181A)

    val Cap = Color(0xFF262B2E)
    val CapLift = Color(0xFF30363A)
    val Paper = Color(0xFFE6E6E2)
    val PaperLift = Color(0xFFF3F3F0)
    val Amber = Color(0xFFE3A72A)
    val AmberLift = Color(0xFFEFBB48)

    val Glyph = Color(0xFFD9DCDE)
    val GlyphOnPaper = Color(0xFF2A2E31)
    val GlyphOnAmber = Color(0xFF2B2007)
    val Muted = Color(0xFF8F989E)
    val Led = Color(0xFFFFFFFF)
    val Online = Color(0xFF6FD99B)
    val Offline = Color(0xFFFF6B5E)
}

private val Colors = darkColorScheme(
    primary = Palette.Paper,
    onPrimary = Palette.GlyphOnPaper,
    background = Palette.PlateBottom,
    onBackground = Palette.Glyph,
    surface = Palette.Cap,
    onSurface = Palette.Glyph,
    surfaceVariant = Palette.Cap,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.Cap,
    surfaceContainerHigh = Palette.Cap,
    error = Palette.Offline,
)

@Composable
fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
