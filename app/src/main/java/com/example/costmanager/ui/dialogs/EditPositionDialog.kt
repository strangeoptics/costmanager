package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Position") },
        text = {
            Column {
                TextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = itemType,
                    onValueChange = { itemType = it },
                    label = { Text("Item Type") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Unit Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedPosition = position.copy(
                    itemName = itemName,
                    itemType = itemType,
                    quantity = quantity.toDoubleOrNull() ?: position.quantity,
                    unit = unit,
                    unitPrice = unitPrice.toDoubleOrNull() ?: position.unitPrice,
                    price = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)
                )
                onConfirm(updatedPosition)
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