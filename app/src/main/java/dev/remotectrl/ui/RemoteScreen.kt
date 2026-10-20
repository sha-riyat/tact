package dev.remotectrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.remotectrl.atv.KeyCodes
import dev.remotectrl.atv.RemoteState
import dev.remotectrl.data.Tv

// Layout in design pixels (a 375 x 937 artboard); everything is scaled by one factor so it fills the screen.
private const val DESIGN_HEIGHT = 753f
private const val DESIGN_WIDTH = 253f
private const val SMALL = 72f
private const val BIG = 100f
private const val ROW_WIDTH = 240f

@Composable
fun RemoteScreen(
    tv: Tv?,
    state: RemoteState,
    saved: List<Tv>,
    found: List<Tv>,
    onOpen: (Tv) -> Unit,
    onKey: (Int) -> Unit,
    onType: (String) -> Unit,
    onReconnect: () -> Unit,
    onRepair: () -> Unit,
    onDevices: () -> Unit,
) {
    var showKeyboard by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        DeviceBar(tv, state, saved, found, onOpen, onDevices, onReconnect, onRepair)
        RemoteKeys(Modifier.weight(1f), tv != null && state == RemoteState.Ready, onKey) { showKeyboard = true }
    }

    if (showKeyboard) KeyboardDialog(onSend = onType, onKey = onKey, onDismiss = { showKeyboard = false })
}

@Composable
private fun RemoteKeys(modifier: Modifier, ready: Boolean, onKey: (Int) -> Unit, onKeyboard: () -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val s = minOf((maxHeight.value - 24f) / DESIGN_HEIGHT, (maxWidth.value - 48f) / DESIGN_WIDTH)

        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.width((ROW_WIDTH * s).dp), horizontalArrangement = Arrangement.SpaceBetween) {
                KeyButton(Glyph.POWER, "Marche/Arrêt", ready, (SMALL * s).dp) { onKey(KeyCodes.POWER) }
                KeyButton(Glyph.KEYBOARD, "Clavier", ready, (SMALL * s).dp, onKeyboard)
            }

            DPadRing(ready, (DESIGN_WIDTH * s).dp, onKey)

            Row(Modifier.width((ROW_WIDTH * s).dp), horizontalArrangement = Arrangement.spacedBy((40f * s).dp)) {
                Column(verticalArrangement = Arrangement.spacedBy((24f * s).dp)) {
                    KeyButton(Glyph.BACK, "Retour", ready, (BIG * s).dp) { onKey(KeyCodes.BACK) }
                    KeyButton(Glyph.PLAY_PAUSE, "Lecture/pause", ready, (BIG * s).dp) { onKey(KeyCodes.PLAY_PAUSE) }
                    KeyButton(Glyph.MUTE, "Muet", ready, (BIG * s).dp) { onKey(KeyCodes.MUTE) }
                }
                Column(verticalArrangement = Arrangement.spacedBy((30f * s).dp)) {
                    KeyButton(Glyph.HOME, "Accueil", ready, (BIG * s).dp) { onKey(KeyCodes.HOME) }
                    VolumeRocker(
                        ready, (BIG * s).dp, (218f * s).dp,
                        onUp = { onKey(KeyCodes.VOLUME_UP) },
                        onDown = { onKey(KeyCodes.VOLUME_DOWN) },
                    )
                }
            }
        }
    }
}
