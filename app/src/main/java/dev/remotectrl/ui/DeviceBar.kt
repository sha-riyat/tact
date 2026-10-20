package dev.remotectrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.remotectrl.atv.RemoteState
import dev.remotectrl.data.Tv

/** The TV name at the top; tapping it opens the list of TVs, and shows what to do when the link drops. */
@Composable
fun DeviceBar(
    tv: Tv?,
    state: RemoteState,
    saved: List<Tv>,
    found: List<Tv>,
    onOpen: (Tv) -> Unit,
    onMore: () -> Unit,
    onReconnect: () -> Unit,
    onRepair: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val nearby = found.filter { f -> saved.none { it.host == f.host } }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Row(
                // With no TV yet there is no list to open: the title goes straight to adding one.
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { if (tv == null) onMore() else open = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tv != null) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor(state)))
                    Spacer(Modifier.width(10.dp))
                }
                Text(tv?.name ?: "Ajouter un appareil", color = Palette.Mark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                GlyphIcon(if (tv == null) Glyph.PLUS else Glyph.CHEVRON_DOWN, Palette.Mark, Modifier.size(16.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                saved.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        trailingIcon = { if (item.host == tv?.host) GlyphIcon(Glyph.CHECK, Palette.Mark, Modifier.size(18.dp)) },
                        onClick = {
                            open = false
                            if (item.host != tv?.host) onOpen(item)
                        },
                    )
                }
                nearby.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("${item.name} · appairer", color = Palette.Muted) },
                        onClick = { open = false; onOpen(item) },
                    )
                }
                HorizontalDivider(color = Palette.KeyHeld)
                DropdownMenuItem(text = { Text("Ajouter un appareil") }, onClick = { open = false; onMore() })
            }
        }

        when (state) {
            RemoteState.Connecting -> Text("Connexion…", color = Palette.Muted, fontSize = 13.sp)
            is RemoteState.Failed -> {
                Text(if (state.needsPairing) "Appairage requis" else "Connexion perdue", color = Palette.Offline, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onReconnect) { Text("Reconnecter") }
                    if (state.needsPairing) TextButton(onClick = onRepair) { Text("Réappairer") }
                }
            }
            else -> Unit
        }
    }
}

private fun dotColor(state: RemoteState): Color = when (state) {
    RemoteState.Ready -> Palette.Online
    is RemoteState.Failed -> Palette.Offline
    else -> Palette.Muted
}
