package dev.remotectrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_DELAY_MS = 450L
private const val REPEAT_MS = 140L

/** Calls [tick] on press, keeps calling it while the finger stays down if [repeat], and reports the pressed state. */
fun Modifier.pressKey(enabled: Boolean, repeat: Boolean, onPressed: (Boolean) -> Unit, tick: () -> Unit): Modifier = composed {
    val current by rememberUpdatedState(tick)
    val report by rememberUpdatedState(onPressed)
    pointerInput(enabled, repeat) {
        if (!enabled) return@pointerInput
        detectTapGestures(onPress = {
            report(true)
            current()
            coroutineScope {
                val job = if (repeat) launch {
                    delay(HOLD_DELAY_MS)
                    while (true) {
                        current()
                        delay(REPEAT_MS)
                    }
                } else null
                tryAwaitRelease()
                job?.cancel()
            }
            report(false)
        })
    }
}

private fun markColor(tone: Tone, enabled: Boolean): Color = if (enabled) tone.glyph else tone.glyph.copy(alpha = 0.35f)

@Composable
fun KeyButton(glyph: Glyph, label: String, enabled: Boolean, size: Dp, tone: Tone = Tone.Dark, onKey: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(size)
            .cap(CircleShape, tone, pressed)
            .semantics { contentDescription = label; role = Role.Button }
            .pressKey(enabled, repeat = false, onPressed = { pressed = it }, tick = onKey),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, markColor(tone, enabled), Modifier.size(size * 0.38f))
    }
}

/** Tall pill split by a hairline seam: + above, − below; hold either half to repeat. */
@Composable
fun VolumeRocker(enabled: Boolean, width: Dp, height: Dp, onUp: () -> Unit, onDown: () -> Unit) {
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .size(width, height)
            .cap(Pill, Tone.Dark, upPressed || downPressed),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RockerHalf(Glyph.PLUS, "Volume plus", enabled, width, onUp) { upPressed = it }
        Box(Modifier.fillMaxWidth(0.5f).height(1.dp).background(Palette.Well))
        RockerHalf(Glyph.MINUS, "Volume moins", enabled, width, onDown) { downPressed = it }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.RockerHalf(
    glyph: Glyph,
    label: String,
    enabled: Boolean,
    width: Dp,
    onTick: () -> Unit,
    onPressed: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .semantics { contentDescription = label; role = Role.Button }
            .pressKey(enabled, repeat = true, onPressed = onPressed, tick = onTick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, markColor(Tone.Dark, enabled), Modifier.size(width * 0.38f))
    }
}
