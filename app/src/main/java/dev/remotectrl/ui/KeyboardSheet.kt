package dev.remotectrl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.remotectrl.atv.diffText

private val Sheet = RoundedCornerShape(24.dp)
private val Field = RoundedCornerShape(16.dp)

/**
 * Text entry for the TV, in the same dark dot-matrix window as the top strip. It sits in the app's own
 * window (not a separate dialog window) and rides above the on-screen keyboard; the Send key sends.
 */
@Composable
fun KeyboardSheet(onInsert: (String) -> Unit, onDelete: (Int) -> Unit, onSubmit: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(onBack = onDismiss)

    // Every change goes to the TV at once, so the letters show up there as they are typed.
    fun change(new: String) {
        val edit = diffText(text, new)
        text = new
        if (edit.deletes > 0) onDelete(edit.deletes)
        if (edit.insert.isNotEmpty()) onInsert(edit.insert)
    }

    fun submit() {
        onSubmit()
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(Sheet)
                .background(Palette.Ink)
                .dotMatrix(Sheet)
                // Taps on the sheet itself must not reach the scrim and close it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("CLAVIER", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Medium)

            val fill = Palette.Well
            TextField(
                value = text,
                onValueChange = ::change,
                placeholder = { Text("Texte à envoyer", color = Palette.Muted, fontSize = 17.sp) },
                textStyle = TextStyle(color = Palette.Glyph, fontSize = 17.sp),
                shape = Field,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Palette.Glyph,
                    unfocusedTextColor = Palette.Glyph,
                    focusedContainerColor = fill,
                    unfocusedContainerColor = fill,
                    cursorColor = Palette.Amber,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )

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
                SubmitKey(::submit)
            }
        }
    }
}

/** Presses Enter on the TV (runs the search, validates the field) and closes the sheet. */
@Composable
private fun SubmitKey(onClick: () -> Unit) {
    val tone = Tone.Paper
    Box(
        modifier = Modifier
            .height(44.dp)
            .cap(Pill, tone)
            .clip(Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Valider",
            color = tone.glyph,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
        )
    }
}
