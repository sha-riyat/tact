package dev.remotectrl.ui

import android.graphics.Bitmap
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** The three cap finishes: matte graphite, bead-blasted paper, and the one amber accent. */
enum class Tone(val base: Color, val lift: Color, val glyph: Color) {
    Dark(Palette.Cap, Palette.CapLift, Palette.Glyph),
    Paper(Palette.Paper, Palette.PaperLift, Palette.GlyphOnPaper),
    Amber(Palette.Amber, Palette.AmberLift, Palette.GlyphOnAmber),
}

/** A 128 px tile of fine noise; tiled over a surface it reads as sand-blasted metal. */
private val GrainBrush: Brush by lazy {
    val n = 128
    val random = Random(7)
    val pixels = IntArray(n * n) {
        val v = random.nextInt(256)
        val alpha = random.nextInt(0, 22)
        (alpha shl 24) or (v shl 16) or (v shl 8) or v
    }
    ShaderBrush(ImageShader(Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888).asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

/** Lays the noise over whatever is drawn below it, clipped to [shape]. */
fun Modifier.grain(shape: Shape = RectangleShape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawWithContent {
        drawContent()
        drawOutline(outline, GrainBrush)
    }
}

/** The faceplate: graphite that is a touch lighter at the top, with grain. */
fun Modifier.plate(): Modifier = this
    .drawWithCache {
        val brush = Brush.verticalGradient(listOf(Palette.PlateTop, Palette.PlateBottom))
        onDrawBehind { drawRect(brush) }
    }
    .grain()

/**
 * A cap set in a dark recess. The cap fades softly from a lit top-left, with no hard edge light;
 * pressed, it sinks deeper into the recess and dims.
 */
fun Modifier.cap(shape: Shape, tone: Tone = Tone.Dark, pressed: Boolean = false): Modifier = drawWithCache {
    val inset = (if (pressed) 4.dp else 2.5.dp).toPx()
    val capSize = Size(size.width - 2 * inset, size.height - 2 * inset)
    val well = shape.createOutline(size, layoutDirection, this)
    val face = shape.createOutline(capSize, layoutDirection, this)
    val fade = Brush.radialGradient(
        listOf(tone.lift, tone.base),
        center = Offset(capSize.width * 0.36f, capSize.height * 0.28f),
        radius = maxOf(capSize.width, capSize.height) * 0.95f,
    )
    onDrawBehind {
        drawOutline(well, Palette.Well)
        translate(inset, inset) {
            drawOutline(face, fade)
            if (pressed) drawOutline(face, Color.Black, alpha = 0.2f)
        }
    }
}.grain(shape)

/** Rows of tiny dots, like the grille of a speaker; used inside the display window. */
fun Modifier.dotMatrix(shape: Shape, color: Color = Color.White.copy(alpha = 0.07f)): Modifier = drawWithCache {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val step = 5.dp.toPx()
    val radius = 0.6.dp.toPx()
    onDrawBehind {
        clipPath(path) {
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(color, radius, Offset(x, y))
                    x += step
                }
                y += step
            }
        }
    }
}

val Pill = androidx.compose.foundation.shape.RoundedCornerShape(CornerSize(50))
