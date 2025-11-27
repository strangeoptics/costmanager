package com.example.costmanager

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.costmanager.data.Purchase
import com.example.costmanager.data.PurchaseWithPositions
import com.example.costmanager.ui.theme.CostManagerTheme
import com.example.costmanager.ui.viewmodel.PurchaseViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class Grouping {
    WEEK, MONTH, YEAR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CostManagerTheme(darkTheme = true) {
                CostManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostManagerApp(purchaseViewModel: PurchaseViewModel = viewModel()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val purchases by purchaseViewModel.allPurchases.collectAsState()
    val undoState by purchaseViewModel.undoState.collectAsState()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val datePickerRequest by purchaseViewModel.datePickerRequest.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var grouping by remember { mutableStateOf(Grouping.MONTH) }

    if (datePickerRequest != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { purchaseViewModel.dismissDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Date(it)
                        val tz = TimeZone.getDefault()
                        val offset = tz.getOffset(selectedDate.time)
                        val correctedDate = Date(selectedDate.time + offset)

                        purchaseViewModel.updatePurchaseDate(datePickerRequest!!.purchaseId, correctedDate)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { purchaseViewModel.dismissDatePicker() }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = { from, to ->
                scope.launch {
                    val json = purchaseViewModel.getPurchasesAsJson(from, to)
                    val file = File(context.cacheDir, "export.json")
                    file.writeText(json)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Daten teilen"))
                }
            },
            onSave = { from, to ->
                scope.launch {
                    val json = purchaseViewModel.getPurchasesAsJson(from, to)
                    val filename = "cost-manager-export-${System.currentTimeMillis()}.json"
                    try {
                        val contentResolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/CostManager")
                            }
                        }
                        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                            outputStream?.use { stream -> stream.write(json.toByteArray()) }
                            Toast.makeText(context, "Export gespeichert in Downloads", Toast.LENGTH_LONG).show()
                        } ?: throw Exception("MediaStore URI was null")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Fehler beim Speichern des Exports", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonString = reader.readText()
                    purchaseViewModel.importPurchasesFromJson(jsonString)
                    Toast.makeText(context, "Import erfolgreich!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Fehler beim Importieren der Datei.", Toast.LENGTH_LONG).show()
                }
            }
        }
    )

    val processUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            isLoading = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                purchaseViewModel.createPurchaseFromImage(bitmap) {
                    isLoading = false
                    Toast.makeText(context, "Einkauf erfolgreich hinzugefügt!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Fehler bei der Verarbeitung des Bildes.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = processUri
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = processUri
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Cost Manager", modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text(text = "Einkäufe") },
                    selected = true,
                    onClick = { /*TODO*/ }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Bild importieren") },
                    selected = false,
                    onClick = {
                        filePickerLauncher.launch("image/*")
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Import") },
                    selected = false,
                    onClick = {
                        importJsonLauncher.launch("application/json")
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Export") },
                    selected = false,
                    onClick = {
                        showExportDialog = true
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text(text = "Einstellungen") },
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                        scope.launch {
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Einkäufe") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if (isClosed) open() else close()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Navigation Drawer")
                        }
                    },
                    actions = {
                        if (undoState != null) {
                            IconButton(onClick = { purchaseViewModel.undoDelete() }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig")
                            }
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Gruppierung ändern")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nach Woche") },
                                onClick = {
                                    grouping = Grouping.WEEK
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nach Monat") },
                                onClick = {
                                    grouping = Grouping.MONTH
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nach Jahr") },
                                onClick = {
                                    grouping = Grouping.YEAR
                                    showMenu = false
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Einkauf hinzufügen")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    PurchaseList(
                        purchases = purchases,
                        purchaseViewModel = purchaseViewModel,
                        grouping = grouping,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

// ... ExportDialog remains the same ...
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PurchaseList(
    purchases: List<PurchaseWithPositions>,
    purchaseViewModel: PurchaseViewModel,
    grouping: Grouping,
    modifier: Modifier = Modifier
) {
    val groupedPurchases = purchases.groupBy {
        val calendar = Calendar.getInstance()
        calendar.time = it.purchase.purchaseDate
        when (grouping) {
            Grouping.YEAR -> calendar.get(Calendar.YEAR)
            Grouping.MONTH -> calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
            Grouping.WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
            }
        }
    }.toSortedMap(compareByDescending { it })

    LazyColumn(modifier = modifier) {
        groupedPurchases.forEach { (_, monthPurchases) ->
            stickyHeader {
                val firstPurchaseInGroup = monthPurchases.first()
                val date = firstPurchaseInGroup.purchase.purchaseDate
                val groupTotal = monthPurchases.sumOf { it.purchase.totalPrice }
                val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                val headerText = when (grouping) {
                    Grouping.YEAR -> localDate.year.toString()
                    Grouping.MONTH -> {
                        val monthName = localDate.month.getDisplayName(TextStyle.FULL, Locale.GERMANY)
                        val year = localDate.year
                        "$monthName $year"
                    }
                    Grouping.WEEK -> {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        calendar.firstDayOfWeek = Calendar.MONDAY
                        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
                        val year = localDate.year
                        "KW $weekOfYear $year"
                    }
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = headerText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${"%.2f".format(groupTotal)} €",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            items(monthPurchases, key = { it.purchase.id }) { purchaseWithPositions ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                            purchaseViewModel.deletePurchase(purchaseWithPositions)
                            true
                        } else {
                            false
                        }
                    },
                    positionalThreshold = { it * 0.75f } // Require 75% swipe
                )

                SwipeToDismissBox(
                    state = dismissState,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) Color.Transparent else Color.Red,
                            label = ""
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                            label = ""
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Icon",
                                modifier = Modifier.scale(scale)
                            )
                        }
                    },
                    content = {
                        PurchaseCard(purchaseWithPositions.purchase)
                    }
                )
            }
        }
    }
}

@Composable
fun PurchaseCard(purchase: Purchase) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, PurchaseDetailActivity::class.java).apply {
                    putExtra("purchaseId", purchase.id)
                }
                context.startActivity(intent)
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = purchase.store, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${"%.2f".format(purchase.totalPrice)} €",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = purchase.storeType, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = dateFormatter.format(purchase.purchaseDate),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CostManagerTheme {
        CostManagerApp()
    }
}
