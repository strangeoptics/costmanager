package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.costmanager.data.Position

@Composable
fun EditPositionDialog(
    position: Position,
    onDismiss: () -> Unit,
    onConfirm: (Position) -> Unit
) {
    var itemName by remember { mutableStateOf(position.itemName) }
    var itemType by remember { mutableStateOf(position.itemType) }
    var quantity by remember { mutableStateOf(position.quantity.toString()) }
    var unit by remember { mutableStateOf(position.unit) }
    var unitPrice by remember { mutableStateOf(position.unitPrice.toString()) }
    var expandedItemType by remember { mutableStateOf(false) }

    val itemTypeSuggestions = listOf("Lebensmittel", "Kleidung", "Treibstoff", "Elektronik", "Baumarkt", "Dekorativ", "Rabatt")

    val isFormValid = itemName.isNotBlank() &&
            quantity.toDoubleOrNull() != null && quantity.toDouble() > 0 &&
            unitPrice.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Position bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Artikelname") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedTextField(
                        value = itemType,
                        onValueChange = { itemType = it; expandedItemType = true },
                        label = { Text("Artikel-Kategorie") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { expandedItemType = !expandedItemType }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") } }
                    )
                    DropdownMenu(expanded = expandedItemType, onDismissRequest = { expandedItemType = false }) {
                        itemTypeSuggestions.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { itemType = suggestion; expandedItemType = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Menge") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Einheit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Preis pro Einheit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedPrice = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)
                    val updatedPosition = position.copy(
                        itemName = itemName,
                        itemType = itemType,
                        quantity = quantity.toDoubleOrNull() ?: position.quantity,
                        unit = unit,
                        unitPrice = unitPrice.toDoubleOrNull() ?: position.unitPrice,
                        price = updatedPrice
                    )
                    onConfirm(updatedPosition)
                },
                enabled = isFormValid
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}