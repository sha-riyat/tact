package dev.remotectrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.remotectrl.atv.RemoteState
import dev.remotectrl.data.Tv

private val Window = RoundedCornerShape(18.dp)

/**
 * The top strip: a dot-matrix window with the TV name (tap for the list of TVs) and, beside it,
 * a round key that reconnects when the link is down.
 */
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
    var windowWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val nearby = found.filter { f -> saved.none { it.host == f.host } }
    val down = state is RemoteState.Failed || state == RemoteState.Idle

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).onSizeChanged { windowWidth = with(density) { it.width.toDp() } }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(Window)
                    .background(Palette.Ink)
                    .dotMatrix(Window)
                    // With no TV yet there is no list to open: the title goes straight to adding one.
                    .clickable { if (tv == null) onMore() else open = true }
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tv != null) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor(state)))
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    tv?.name ?: "Ajouter un appareil",
                    color = Palette.Glyph,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                GlyphIcon(if (tv == null) Glyph.PLUS else Glyph.CHEVRON_DOWN, Palette.Muted, Modifier.size(14.dp))
            }

            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.width(windowWidth).dotMatrix(Window),
                offset = DpOffset(0.dp, 6.dp),
                shape = Window,
                containerColor = Palette.Ink,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                saved.forEach { item ->
                    MenuRow(
                        label = item.name,
                        current = item.host == tv?.host,
                        onClick = {
                            open = false
                            if (item.host != tv?.host) onOpen(item)
                        },
                    )
                }
                nearby.forEach { item ->
                    MenuRow(label = "${item.name} · appairer", muted = true, onClick = { open = false; onOpen(item) })
                }
                HorizontalDivider(color = Palette.Well, modifier = Modifier.padding(horizontal = 14.dp))
                MenuRow(label = "Ajouter un appareil", glyph = Glyph.PLUS, onClick = { open = false; onMore() })
            }
        }

        if (tv != null) {
            val repair = (state as? RemoteState.Failed)?.needsPairing == true
            // Same dark dot-matrix as the name window; the glyph turns amber when there is something to do.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Palette.Ink)
                    .dotMatrix(CircleShape)
                    .semantics { contentDescription = "Reconnecter"; role = Role.Button }
                    .clickable(enabled = down, onClick = if (repair) onRepair else onReconnect),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.REFRESH, if (down) Palette.Amber else Palette.Muted.copy(alpha = 0.6f), Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, current: Boolean = false, muted: Boolean = false, glyph: Glyph? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (muted) Palette.Muted else Palette.Glyph,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                maxLines = 1,
            )
        },
        leadingIcon = glyph?.let { { GlyphIcon(it, Palette.Muted, Modifier.size(16.dp)) } },
        trailingIcon = if (current) ({ GlyphIcon(Glyph.CHECK, Palette.Glyph, Modifier.size(16.dp)) }) else null,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    )
}

private fun dotColor(state: RemoteState): Color = when (state) {
    RemoteState.Ready -> Palette.Online
    is RemoteState.Failed -> Palette.Offline
    else -> Palette.Muted
}
