package com.patrykadamski.waterreminder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Draft (string-backed) form of a [VesselSize] so a mid-edit, momentarily
 * invalid amount field doesn't force a parse/crash - conversion back to a
 * real VesselSize only happens when the settings dialog is saved.
 */
data class VesselSizeDraft(val id: Long, val name: String, val amountText: String)

fun List<VesselSize>.toDrafts(): List<VesselSizeDraft> =
    map { VesselSizeDraft(it.id, it.name, it.amountMl.toString()) }

fun List<VesselSizeDraft>.toVesselSizes(): List<VesselSize> =
    mapNotNull { draft ->
        val amount = draft.amountText.toIntOrNull()
        if (draft.name.isBlank() || amount == null || amount <= 0) null
        else VesselSize(id = draft.id, name = draft.name.trim(), amountMl = amount)
    }

@Composable
fun VesselSizeEditor(
    sizes: List<VesselSizeDraft>,
    onSizesChange: (List<VesselSizeDraft>) -> Unit
) {
    Column {
        Text("Szybkie przyciski logowania", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))

        sizes.forEach { draft ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { newName ->
                        onSizesChange(sizes.map { if (it.id == draft.id) it.copy(name = newName) else it })
                    },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = draft.amountText,
                    onValueChange = { newAmount ->
                        if (newAmount.length <= 5 && newAmount.all(Char::isDigit)) {
                            onSizesChange(sizes.map { if (it.id == draft.id) it.copy(amountText = newAmount) else it })
                        }
                    },
                    label = { Text("ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp)
                )
                IconButton(
                    onClick = { onSizesChange(sizes.filterNot { it.id == draft.id }) },
                    enabled = sizes.size > 1
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń ${draft.name}")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        var newName by remember { mutableStateOf("") }
        var newAmount by remember { mutableStateOf("") }
        val canAdd = newName.isNotBlank() && (newAmount.toIntOrNull() ?: 0) > 0

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nowa nazwa") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = newAmount,
                onValueChange = { if (it.length <= 5 && it.all(Char::isDigit)) newAmount = it },
                label = { Text("ml") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            IconButton(
                onClick = {
                    onSizesChange(sizes + VesselSizeDraft(id = VesselSizePrefs.newId(), name = newName.trim(), amountText = newAmount))
                    newName = ""
                    newAmount = ""
                },
                enabled = canAdd
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj rozmiar")
            }
        }
    }
}
