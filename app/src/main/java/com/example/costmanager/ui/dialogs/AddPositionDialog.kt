package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AddPositionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, Double) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Stück") }
    var unitPrice by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val itemTypeSuggestions = listOf("Lebensmittel", "Kleidung", "Treibstoff", "Elektronik", "Baumarkt", "Dekorativ")

    val isFormValid = itemName.isNotBlank() &&
            quantity.toDoubleOrNull() != null && quantity.toDouble() > 0 &&
            unitPrice.toDoubleOrNull() != null && unitPrice.toDouble() >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Position hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Artikelname") }
                )
                Box {
                    OutlinedTextField(
                        value = itemType,
                        onValueChange = {
                            itemType = it
                            expanded = true
                        },
                        label = { Text("Kategorie") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        itemTypeSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    itemType = suggestion
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Menge") },
                        modifier = androidx.compose.ui.Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Einheit") },
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Preis pro Einheit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        itemName,
                        itemType,
                        quantity.toDouble(),
                        unit,
                        unitPrice.toDouble()
                    )
                },
                enabled = isFormValid
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}