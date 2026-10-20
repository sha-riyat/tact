package dev.remotectrl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.remotectrl.data.Tv

/** First screen: pick the TV to control. Saved TVs connect at once, new ones go through pairing. */
@Composable
fun DevicesScreen(
    saved: List<Tv>,
    found: List<Tv>,
    onOpen: (Tv) -> Unit,
    onForget: (Tv) -> Unit,
    onManual: (String) -> Unit,
) {
    val nearby = found.filter { f -> saved.none { it.host == f.host } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Choisis un téléviseur",
                color = Palette.Glyph,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }
        if (saved.isNotEmpty()) item { SectionTitle("Mes téléviseurs") }
        items(saved, key = { "s-" + it.host }) { tv -> TvRow(tv, null, onOpen, onForget) }

        item { SectionTitle("À proximité") }
        if (nearby.isEmpty()) item { Text("Recherche sur le Wi-Fi… Le téléviseur doit être allumé.", color = Palette.Muted, fontSize = 14.sp) }
        items(nearby, key = { "n-" + it.host }) { tv -> TvRow(tv, "Appairer", onOpen, null) }

        item { ManualEntry(onManual) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Palette.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, start = 4.dp))
}

@Composable
private fun TvRow(tv: Tv, hint: String?, onOpen: (Tv) -> Unit, onForget: ((Tv) -> Unit)?) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cap(shape)
            .clip(shape)
            .clickable { onOpen(tv) }
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tv.name, color = Palette.Glyph, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(tv.host, color = Palette.Muted, fontSize = 13.sp)
        }
        if (hint != null) Text(hint, color = Palette.Muted, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
        if (onForget != null) TextButton(onClick = { onForget(tv) }) { Text("Oublier", color = Palette.Muted) }
    }
}

@Composable
private fun ManualEntry(onManual: (String) -> Unit) {
    var host by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 24.dp)) {
        SectionTitle("Adresse IP")
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Ex. 192.168.1.20") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onManual(host) }, enabled = host.isNotBlank()) { Text("Ajouter") }
    }
}
