package com.example.costmanager.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.costmanager.ui.viewmodel.PositionInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Date, PositionInput?) -> Unit
) {
    // Purchase state
    var store by remember { mutableStateOf("") }
    var storeType by remember { mutableStateOf("") }
    var expandedStoreType by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Position state
    var addPosition by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var itemType by remember { mutableStateOf("") }
    var expandedItemType by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Stück") }
    var unitPrice by remember { mutableStateOf("") }

    val storeTypeOptions = listOf("Supermarkt", "Tankstelle", "Klamottenladen", "Baumarkt", "Unbekannt")
    val itemTypeSuggestions = listOf("Lebensmittel", "Kleidung", "Treibstoff", "Elektronik", "Baumarkt", "Dekorativ")
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    val isPositionDataValid = !addPosition || (
            itemName.isNotBlank() &&
            quantity.toDoubleOrNull() != null && quantity.toDouble() > 0 &&
            unitPrice.toDoubleOrNull() != null && unitPrice.toDouble() >= 0
            )
    val isFormValid = store.isNotBlank() && isPositionDataValid

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val rawDate = Date(it)
                        val tz = TimeZone.getDefault()
                        val offset = tz.getOffset(rawDate.time)
                        selectedDate = Date(rawDate.time + offset)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Einkauf erstellen") },
        text = {
            Column {
                // --- Purchase Fields ---
                OutlinedTextField(value = store, onValueChange = { store = it }, label = { Text("Geschäft") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = storeType,
                        onValueChange = { storeType = it; expandedStoreType = true },
                        label = { Text("Kategorie") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { expandedStoreType = !expandedStoreType }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") } }
                    )
                    DropdownMenu(expanded = expandedStoreType, onDismissRequest = { expandedStoreType = false }, modifier = Modifier.fillMaxWidth()) {
                        storeTypeOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { storeType = option; expandedStoreType = false })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val interactionSource = remember { MutableInteractionSource() }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) { showDatePicker = true }
                    }
                }
                OutlinedTextField(value = dateFormatter.format(selectedDate), onValueChange = {}, label = { Text("Datum") }, readOnly = true, trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Datum wählen") }, modifier = Modifier.fillMaxWidth(), interactionSource = interactionSource)
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // --- Add Position Checkbox ---
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { addPosition = !addPosition }) {
                    Checkbox(checked = addPosition, onCheckedChange = { addPosition = it })
                    Text("Position hinzufügen")
                }

                // --- Optional Position Fields ---
                AnimatedVisibility(visible = addPosition) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Artikelname") }, modifier = Modifier.fillMaxWidth())
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
                            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Menge") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Einheit") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Preis pro Einheit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val positionInput = if (addPosition) {
                        PositionInput(
                            itemName = itemName,
                            itemType = itemType,
                            quantity = quantity.toDouble(),
                            unit = unit,
                            unitPrice = unitPrice.toDouble()
                        )
                    } else {
                        null
                    }
                    onConfirm(store, storeType, selectedDate, positionInput)
                },
                enabled = isFormValid
            ) { Text("Erstellen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}