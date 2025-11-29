package com.example.costmanager

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.costmanager.data.Purchase
import com.example.costmanager.data.PurchaseWithPositions
import com.example.costmanager.ui.dialogs.EditPurchaseDialog
import com.example.costmanager.ui.dialogs.ExportDialog
import com.example.costmanager.ui.dialogs.ManualPurchaseDialog
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
    DAY, WEEK, MONTH, YEAR
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

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY).format(Date())
    val storageDir = context.cacheDir
    return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
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
    var showManualPurchaseDialog by remember { mutableStateOf(false) }
    var showEditPurchaseDialog by remember { mutableStateOf<Purchase?>(null) }
    var grouping by remember { mutableStateOf(Grouping.MONTH) }

    val sharedPreferences = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var initialLoadSize by remember {
        mutableStateOf(sharedPreferences.getInt("initial_load_size", 3))
    }
    var subsequentLoadSize by remember {
        mutableStateOf(sharedPreferences.getInt("subsequent_load_size", 3))
    }

    var visibleItemCount by remember { mutableStateOf(initialLoadSize) }

    LaunchedEffect(key1 = Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "initial_load_size" -> initialLoadSize = prefs.getInt(key, 3)
                "subsequent_load_size" -> subsequentLoadSize = prefs.getInt(key, 3)
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }


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

    if (showManualPurchaseDialog) {
        ManualPurchaseDialog(
            onDismiss = { showManualPurchaseDialog = false },
            onConfirm = { store, storeType, date, positionInput ->
                purchaseViewModel.addPurchase(store, storeType, date, positionInput)
                showManualPurchaseDialog = false
            }
        )
    }

    showEditPurchaseDialog?.let { purchase ->
        EditPurchaseDialog(
            purchase = purchase,
            onDismiss = { showEditPurchaseDialog = null },
            onConfirm = { updatedPurchase ->
                purchaseViewModel.updatePurchase(updatedPurchase)
                showEditPurchaseDialog = null
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
                                text = { Text("Nach Tag") },
                                onClick = {
                                    grouping = Grouping.DAY
                                    showMenu = false
                                }
                            )
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
                var isFabMenuExpanded by remember { mutableStateOf(false) }
                var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture(),
                    onResult = { success ->
                        if (success) {
                            tempPhotoUri?.let(processUri)
                        }
                    }
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(visible = isFabMenuExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                    isFabMenuExpanded = false
                                },
                            ) {
                                Icon(Icons.Default.Image, contentDescription = "Purchase aus einem Bild in der Fotogallerie erzeugen")
                            }
                            FloatingActionButton(
                                onClick = {
                                    showManualPurchaseDialog = true
                                    isFabMenuExpanded = false
                                },
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Purchase über einen Dialog erstellen")
                            }
                            FloatingActionButton(
                                onClick = {
                                    // TODO: Purchase über Sprechen erzeugen
                                    Toast.makeText(context, "TODO: Spracheingabe", Toast.LENGTH_SHORT).show()
                                    isFabMenuExpanded = false
                                },
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Purchase über Sprechen erzeugen")
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = {}
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isFabMenuExpanded) {
                                            isFabMenuExpanded = false
                                        } else {
                                            val photoFile = createImageFile(context)
                                            val photoURI = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                photoFile
                                            )
                                            tempPhotoUri = photoURI
                                            cameraLauncher.launch(photoURI)
                                        }
                                    },
                                    onLongClick = {
                                        isFabMenuExpanded = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFabMenuExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isFabMenuExpanded) "Menü schließen" else "Einkauf hinzufügen (lange drücken für Menü)"
                            )
                        }
                    }
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
                        visibleItemCount = visibleItemCount,
                        onLoadMore = {
                            visibleItemCount += subsequentLoadSize
                        },
                        onPurchaseLongClick = { purchase ->
                            showEditPurchaseDialog = purchase
                        },
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PurchaseList(
    purchases: List<PurchaseWithPositions>,
    purchaseViewModel: PurchaseViewModel,
    grouping: Grouping,
    visibleItemCount: Int,
    onLoadMore: () -> Unit,
    onPurchaseLongClick: (Purchase) -> Unit,
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
            Grouping.DAY -> calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
        }
    }.toSortedMap(compareByDescending { it })

    LazyColumn(modifier = modifier) {
        val flatPurchases = groupedPurchases.values.flatten()
        val visiblePurchases = flatPurchases.take(visibleItemCount)

        visiblePurchases.groupBy {
            val calendar = Calendar.getInstance()
            calendar.time = it.purchase.purchaseDate
            when (grouping) {
                Grouping.YEAR -> calendar.get(Calendar.YEAR)
                Grouping.MONTH -> calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
                Grouping.WEEK -> {
                    calendar.firstDayOfWeek = Calendar.MONDAY
                    calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
                }
                Grouping.DAY -> calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
            }
        }.toSortedMap(compareByDescending { it }).forEach { (_, monthPurchases) ->
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
                    Grouping.DAY -> {
                        val dateFormatter = SimpleDateFormat("E, dd.MM.yyyy", Locale.GERMANY)
                        dateFormatter.format(date)
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
                        PurchaseCard(
                            purchase = purchaseWithPositions.purchase,
                            onLongClick = { onPurchaseLongClick(purchaseWithPositions.purchase) }
                        )
                    }
                )
            }
        }
        if (visibleItemCount < purchases.size) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Mehr laden")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PurchaseCard(purchase: Purchase, onLongClick: (Purchase) -> Unit) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val intent = Intent(context, PurchaseDetailActivity::class.java).apply {
                        putExtra("purchaseId", purchase.id)
                    }
                    context.startActivity(intent)
                },
                onLongClick = { onLongClick(purchase) }
            )
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