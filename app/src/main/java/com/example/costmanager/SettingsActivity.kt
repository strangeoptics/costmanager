package com.example.costmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.costmanager.data.SettingsManager
import com.example.costmanager.ui.theme.CostManagerTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CostManagerTheme(darkTheme = true) {
                SettingsScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val sharedPreferences = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }


    var initialLoadSize by remember {
        mutableStateOf(sharedPreferences.getInt("initial_load_size", 3).toString())
    }
    var subsequentLoadSize by remember {
        mutableStateOf(sharedPreferences.getInt("subsequent_load_size", 3).toString())
    }
    var apiKey by remember {
        mutableStateOf(settingsManager.getApiKey())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = initialLoadSize,
                onValueChange = {
                    initialLoadSize = it
                    sharedPreferences.edit().putInt("initial_load_size", it.toIntOrNull() ?: 3).apply()
                },
                label = { Text("Anzahl initial geladener Einkäufe") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = subsequentLoadSize,
                onValueChange = {
                    subsequentLoadSize = it
                    sharedPreferences.edit().putInt("subsequent_load_size", it.toIntOrNull() ?: 3).apply()
                },
                label = { Text("Anzahl nachgeladener Einkäufe") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    settingsManager.saveApiKey(it)
                },
                label = { Text("Google API Key") }
            )
        }
    }
}
