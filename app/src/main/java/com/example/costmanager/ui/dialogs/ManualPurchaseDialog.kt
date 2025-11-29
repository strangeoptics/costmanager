package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPurchaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Date) -> Unit
) {
    var store by remember { mutableStateOf("") }
    var storeType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val options = listOf("Supermarkt", "Tankstelle", "Klamottenladen", "Baumarkt", "Unbekannt")
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

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
                        // Adjust for timezone offset similar to other parts of the app
                        val tz = TimeZone.getDefault()
                        val offset = tz.getOffset(rawDate.time)
                        selectedDate = Date(rawDate.time + offset)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
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
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("Geschäft") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = storeType,
                        onValueChange = {
                            storeType = it
                            expanded = true
                        },
                        label = { Text("Kategorie") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    storeType = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Date Picker Field
                val interactionSource = remember { MutableInteractionSource() }
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            showDatePicker = true
                        }
                    }
                }

                OutlinedTextField(
                    value = dateFormatter.format(selectedDate),
                    onValueChange = { },
                    label = { Text("Datum") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Datum wählen")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interactionSource
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (store.isNotBlank()) {
                        onConfirm(store, storeType, selectedDate)
                    }
                }
            ) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}