package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.TradeDirection
import com.example.data.model.VipTradePost

@Composable
fun VipTradeEditorDialog(
    initial: VipTradePost?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        body: String,
        pair: String,
        direction: TradeDirection,
        entry: String,
        sl: String,
        tp: String,
        imageUrl: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var body by remember { mutableStateOf(initial?.body ?: "") }
    var pair by remember { mutableStateOf(initial?.pair ?: "EUR/USD") }
    var direction by remember { mutableStateOf(TradeDirection.fromRaw(initial?.direction)) }
    var entry by remember { mutableStateOf(initial?.entryPrice ?: "") }
    var sl by remember { mutableStateOf(initial?.stopLoss ?: "") }
    var tp by remember { mutableStateOf(initial?.takeProfit ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Publish VIP Trade" else "Edit VIP Trade") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Signal title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pair,
                    onValueChange = { pair = it.uppercase() },
                    label = { Text("Currency pair (e.g. EUR/USD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TradeDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = direction == dir,
                            onClick = { direction = dir },
                            label = { Text(dir.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = entry,
                        onValueChange = { entry = it },
                        label = { Text("Entry") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sl,
                        onValueChange = { sl = it },
                        label = { Text("SL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tp,
                        onValueChange = { tp = it },
                        label = { Text("TP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Signal notes (text only)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Optional chart image URL (no video)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { imageUrl = "local:img_chart_eurusd" }) {
                        Text("Use EUR/USD Chart")
                    }
                    TextButton(onClick = { imageUrl = "local:img_chart_xauusd" }) {
                        Text("Use Gold Chart")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, body, pair, direction, entry, sl, tp, imageUrl.ifBlank { null }) },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) {
                Text(if (initial == null) "Publish" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AnalysisEditorDialog(
    initial: MarketAnalysisPost?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        pair: String,
        timeframe: String,
        summary: String,
        supportsCsv: String,
        resistancesCsv: String,
        imageUrl: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var pair by remember { mutableStateOf(initial?.pair ?: "EUR/USD") }
    var timeframe by remember { mutableStateOf(initial?.timeframe ?: "15M") }
    var summary by remember { mutableStateOf(initial?.summary ?: "") }
    var supportsCsv by remember { mutableStateOf(initial?.supportLevels?.joinToString(", ") ?: "") }
    var resistancesCsv by remember { mutableStateOf(initial?.resistanceLevels?.joinToString(", ") ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Publish Market Analysis" else "Edit Market Analysis") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Analysis title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pair,
                        onValueChange = { pair = it.uppercase() },
                        label = { Text("Pair") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeframe,
                        onValueChange = { timeframe = it.uppercase() },
                        label = { Text("Timeframe") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Chart breakdown") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = resistancesCsv,
                    onValueChange = { resistancesCsv = it },
                    label = { Text("Resistance levels (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supportsCsv,
                    onValueChange = { supportsCsv = it },
                    label = { Text("Support levels (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Optional chart image URL (no video)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { imageUrl = "local:img_chart_eurusd" }) {
                        Text("Use EUR/USD Chart")
                    }
                    TextButton(onClick = { imageUrl = "local:img_chart_xauusd" }) {
                        Text("Use Gold Chart")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, pair, timeframe, summary, supportsCsv, resistancesCsv, imageUrl.ifBlank { null })
                },
                enabled = title.isNotBlank() && pair.isNotBlank() && summary.isNotBlank()
            ) {
                Text(if (initial == null) "Publish" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ChangeAdminPasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (currentPw: String, newPw: String, confirmPw: String) -> Unit
) {
    var currentPw by remember { mutableStateOf("") }
    var newPw by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Admin Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Minimum 8 characters. Stored with salted PBKDF2-HMAC-SHA256.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentPw,
                    onValueChange = { currentPw = it },
                    label = { Text("Current password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it },
                    label = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it },
                    label = { Text("Confirm new password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onChangePassword(currentPw, newPw, confirmPw) },
                enabled = currentPw.isNotBlank() && newPw.length >= 8 && confirmPw.isNotBlank()
            ) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
