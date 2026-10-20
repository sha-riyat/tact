package dev.remotectrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.remotectrl.PairingUi
import dev.remotectrl.data.Tv

@Composable
fun PairingScreen(
    tv: Tv,
    state: PairingUi,
    onSubmit: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Appairage", style = MaterialTheme.typography.headlineMedium)
        Text(tv.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        when (state) {
            PairingUi.Starting, PairingUi.Verifying -> {
                CircularProgressIndicator()
                Text(if (state == PairingUi.Starting) "Connexion au téléviseur…" else "Vérification du code…")
            }
            PairingUi.WaitingCode -> CodeEntry(onSubmit)
            is PairingUi.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                if (state.retryable) Button(onClick = onRetry) { Text("Réessayer") }
            }
        }
        TextButton(onClick = onCancel) { Text("Annuler") }
    }
}

@Composable
private fun CodeEntry(onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    Text(
        "Saisis le code de 6 caractères affiché sur le téléviseur.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = code,
        onValueChange = { v -> code = v.filter { it.isLetterOrDigit() }.take(6).uppercase() },
        label = { Text("Code") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { onSubmit(code) }, enabled = code.length == 6) { Text("Valider") }
}
