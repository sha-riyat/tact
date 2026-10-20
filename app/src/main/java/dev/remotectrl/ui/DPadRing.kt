package dev.remotectrl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.remotectrl.atv.KeyCodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val INNER_RATIO = 0.573f
private const val DOT_REACH = 0.885f
private const val DOT_RATIO = 0.032f
private const val HOLD_DELAY_MS = 450L
private const val REPEAT_MS = 140L

/** The scroll wheel: tap a quarter for an arrow, tap the centre for OK, hold to repeat. */
@Composable
fun DPadRing(enabled: Boolean, diameter: Dp, onKey: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = Modifier
            .size(diameter)
            .semantics { contentDescription = "Croix directionnelle" }
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
        drawOuter(r, pressed)
        drawInner(r * INNER_RATIO, pressed == KeyCodes.CENTER)
        drawDots(r, if (enabled) 0.8f else 0.3f)
    }
}

private fun keyAt(pos: Offset, w: Float, h: Float): Int {
    val dx = pos.x - w / 2
    val dy = pos.y - h / 2
    val r = minOf(w, h) / 2
    return when {
        dx * dx + dy * dy < (r * INNER_RATIO) * (r * INNER_RATIO) -> KeyCodes.CENTER
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

/** Flat ring; the pressed quarter is lit. */
private fun DrawScope.drawOuter(r: Float, pressed: Int?) {
    drawCircle(Palette.Key, r)
    sectorStart(pressed)?.let { start ->
        drawArc(Palette.KeyHeld, start, 90f, true, Offset.Zero, Size(2 * r, 2 * r))
    }
}

/** Flat centre button, same colour as a pressed key so it reads as raised from the ring. */
private fun DrawScope.drawInner(r: Float, pressed: Boolean) {
    drawCircle(if (pressed) Palette.KeyHeld else Palette.KeyPressed, r)
}

private fun DrawScope.drawDots(r: Float, alpha: Float) {
    val dot = Color(0xFFC4C4C4).copy(alpha = alpha)
    listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f).forEach { (dx, dy) ->
        drawCircle(dot, r * DOT_RATIO, Offset(center.x + dx * r * DOT_REACH, center.y + dy * r * DOT_REACH))
    }
}
