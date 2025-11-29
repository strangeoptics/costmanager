package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.costmanager.data.Purchase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    onDismiss: () -> Unit,
    onConfirm: (Purchase) -> Unit
) {
    var store by remember { mutableStateOf(purchase.store) }
    var storeType by remember { mutableStateOf(purchase.storeType) }
    var purchaseDate by remember { mutableStateOf(purchase.purchaseDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = purchaseDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        purchaseDate = Date(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Purchase") },
        text = {
            Column {
                TextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("Store") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = storeType,
                    onValueChange = { storeType = it },
                    label = { Text("Store Type") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showDatePicker = true }) {
                    Text(text = "Date: ${dateFormatter.format(purchaseDate)}")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedPurchase = purchase.copy(
                    store = store,
                    storeType = storeType,
                    purchaseDate = purchaseDate
                )
                onConfirm(updatedPurchase)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}