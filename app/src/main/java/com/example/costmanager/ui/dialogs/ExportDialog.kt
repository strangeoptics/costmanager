package com.example.costmanager.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onShare: (Date, Date) -> Unit,
    onSave: (Date, Date) -> Unit
) {
    var fromDate by remember { mutableStateOf(Date(0)) } // Start of epoch
    var toDate by remember { mutableStateOf(Date()) } // Now
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daten exportieren", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        fromDate = Date(0)
                        toDate = Date()
                    }, modifier = Modifier.weight(1f)) {
                        Text("Alle")
                    }
                    Button(onClick = {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.MONTH, -1)
                        fromDate = cal.time
                        toDate = Date()
                    }, modifier = Modifier.weight(1f)) {
                        Text("Letzter Monat")
                    }
                    Button(onClick = {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.YEAR, -1)
                        fromDate = cal.time
                        toDate = Date()
                    }, modifier = Modifier.weight(1f)) {
                        Text("Letztes Jahr")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Von: ${dateFormatter.format(fromDate)}")
                Text("Bis: ${dateFormatter.format(toDate)}")
                Spacer(modifier = Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onShare(fromDate, toDate); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Teilen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(fromDate, toDate); onDismiss() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}