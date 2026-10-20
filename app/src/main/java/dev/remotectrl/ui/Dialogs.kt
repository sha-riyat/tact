package dev.remotectrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.remotectrl.atv.KeyCodes

private val Sheet = RoundedCornerShape(24.dp)
private val Field = RoundedCornerShape(16.dp)

/** Text entry for the TV, in the same dark dot-matrix window as the top strip. */
@Composable
fun KeyboardDialog(onSend: (String) -> Unit, onKey: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(Sheet)
                .background(Palette.Ink)
                .dotMatrix(Sheet)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("CLAVIER", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)

            Box(Modifier.fillMaxWidth().clip(Field).background(Palette.Well).padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (text.isEmpty()) Text("Texte à envoyer", color = Palette.Muted, fontSize = 17.sp)
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = Palette.Glyph, fontSize = 17.sp),
                    cursorBrush = SolidColor(Palette.Amber),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            Text(
                "Lettres, chiffres et ponctuation simple, en minuscules.",
                color = Palette.Muted,
                fontSize = 12.sp,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextKey("Effacer", Tone.Dark, true) { onKey(KeyCodes.DELETE) }
                TextKey("Entrée", Tone.Dark, true) { onKey(KeyCodes.ENTER) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Fermer",
                    color = Palette.Muted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(Field).clickable(onClick = onDismiss).padding(horizontal = 8.dp, vertical = 12.dp),
                )
                TextKey("Envoyer", Tone.Paper, text.isNotEmpty()) { onSend(text); text = "" }
            }
        }
    }
}

@Composable
private fun TextKey(label: String, tone: Tone, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .cap(Pill, tone)
            .clip(Pill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) tone.glyph else tone.glyph.copy(alpha = 0.35f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}
