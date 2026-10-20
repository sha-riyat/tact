package dev.remotectrl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.remotectrl.atv.KeyCodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val KNOB_RATIO = 0.54f
private const val DOT_REACH = 0.79f
private const val DOT_RATIO = 0.026f
private const val HOLD_DELAY_MS = 450L
private const val REPEAT_MS = 140L

/** The wheel: a knob in a round recess. Tap a quarter for an arrow (its LED lights), the knob for OK, hold to repeat. */
@Composable
fun DPadRing(enabled: Boolean, diameter: Dp, onKey: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = Modifier
            .size(diameter)
            .semantics { contentDescription = "Croix directionnelle" }
            .grain(CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = { pos ->
                    val code = keyAt(pos, size.width.toFloat(), size.height.toFloat())
                    pressed = code
                    onKey(code)
                    val repeat = scope.launch {
                        delay(HOLD_DELAY_MS)
                        while (true) {
                            onKey(code)
                            delay(REPEAT_MS)
                        }
                    }
                    tryAwaitRelease()
                    repeat.cancel()
                    pressed = null
                })
            },
    ) {
        val r = size.minDimension / 2
        drawWell(r, pressed)
        drawKnob(r * KNOB_RATIO, pressed == KeyCodes.CENTER)
        drawLeds(r, pressed, enabled)
    }
}

private fun keyAt(pos: Offset, w: Float, h: Float): Int {
    val dx = pos.x - w / 2
    val dy = pos.y - h / 2
    val r = minOf(w, h) / 2
    return when {
        dx * dx + dy * dy < (r * KNOB_RATIO) * (r * KNOB_RATIO) -> KeyCodes.CENTER
        abs(dx) >= abs(dy) -> if (dx > 0) KeyCodes.RIGHT else KeyCodes.LEFT
        else -> if (dy > 0) KeyCodes.DOWN else KeyCodes.UP
    }
}

/** Canvas angles run clockwise from the +x axis, so each quarter spans 90 degrees. */
private fun sectorStart(key: Int?): Float? = when (key) {
    KeyCodes.RIGHT -> -45f
    KeyCodes.DOWN -> 45f
    KeyCodes.LEFT -> 135f
    KeyCodes.UP -> 225f
    else -> null
}

/** The recess: dark at the rim, a little lighter in the dish, and a darker quarter under the finger. */
private fun DrawScope.drawWell(r: Float, pressed: Int?) {
    drawCircle(Palette.Well, r)
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF1F2427), Palette.Well), center, r),
        r * 0.97f,
    )
    sectorStart(pressed)?.let { start ->
        drawArc(Color.Black.copy(alpha = 0.22f), start, 90f, true, Offset.Zero, Size(2 * r, 2 * r))
    }
}

/** The knob: a matte cap with a soft shadow built from a few faint discs, sinking slightly when pressed. */
private fun DrawScope.drawKnob(full: Float, pressed: Boolean) {
    val r = if (pressed) full * 0.975f else full
    val drop = r * 0.1f
    drawCircle(Color.Black.copy(alpha = 0.10f), r * 1.14f, center.copy(y = center.y + drop))
    drawCircle(Color.Black.copy(alpha = 0.16f), r * 1.07f, center.copy(y = center.y + drop * 0.7f))
    drawCircle(
        Brush.radialGradient(
            listOf(Palette.CapLift, Palette.Cap),
            Offset(center.x - r * 0.25f, center.y - r * 0.35f),
            r * 1.5f,
        ),
        r,
    )
    if (pressed) drawCircle(Color.Black.copy(alpha = 0.2f), r)
}

/** Four tiny LEDs beside the knob; the one for the pressed direction glows. */
private fun DrawScope.drawLeds(r: Float, pressed: Int?, enabled: Boolean) {
    val leds = listOf(KeyCodes.UP to (0f to -1f), KeyCodes.DOWN to (0f to 1f), KeyCodes.LEFT to (-1f to 0f), KeyCodes.RIGHT to (1f to 0f))
    for ((key, dir) in leds) {
        val at = Offset(center.x + dir.first * r * DOT_REACH, center.y + dir.second * r * DOT_REACH)
        val lit = key == pressed
        if (lit) drawCircle(Palette.Led.copy(alpha = 0.22f), r * DOT_RATIO * 3.2f, at)
        drawCircle(if (lit) Palette.Led else Palette.Glyph.copy(alpha = if (enabled) 0.6f else 0.25f), r * DOT_RATIO, at)
    }
}
