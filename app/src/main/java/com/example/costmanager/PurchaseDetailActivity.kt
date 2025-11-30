package com.example.costmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.costmanager.data.Position
import com.example.costmanager.data.Purchase
import com.example.costmanager.data.PurchaseWithPositions
import com.example.costmanager.ui.dialogs.AddPositionDialog
import com.example.costmanager.ui.dialogs.EditPositionDialog
import com.example.costmanager.ui.dialogs.EditPurchaseDialog
import com.example.costmanager.ui.theme.CostManagerTheme
import com.example.costmanager.ui.viewmodel.PurchaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PurchaseDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val purchaseId = intent.getLongExtra("purchaseId", -1L)

        setContent {
            CostManagerTheme(darkTheme = true) {
                if (purchaseId != -1L) {
                    PurchaseDetailScreen(purchaseId = purchaseId) {
                        finish()
                    }
                } else {
                    finish() // Close if no valid ID is provided
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    purchaseId: Long,
    purchaseViewModel: PurchaseViewModel = viewModel(),
    onBack: () -> Unit
) {
    val purchaseWithPositions by purchaseViewModel.getPurchase(purchaseId).collectAsState()
    val undoState by purchaseViewModel.undoState.collectAsState()
    var showAddPositionDialog by remember { mutableStateOf(false) }
    var showEditPurchaseDialog by remember { mutableStateOf<Purchase?>(null) }
    var showEditPositionDialog by remember { mutableStateOf<Position?>(null) }
    val context = LocalContext.current

    if (showAddPositionDialog) {
        AddPositionDialog(
            onDismiss = { showAddPositionDialog = false },
            onConfirm = { itemName, itemType, quantity, unit, unitPrice ->
                purchaseViewModel.addPosition(purchaseId, itemName, itemType, quantity, unit, unitPrice)
                showAddPositionDialog = false
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

    showEditPositionDialog?.let { position ->
        EditPositionDialog(
            position = position,
            onDismiss = { showEditPositionDialog = null },
            onConfirm = { updatedPosition ->
                purchaseViewModel.updatePosition(updatedPosition)
                showEditPositionDialog = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(purchaseWithPositions?.purchase?.store ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (undoState != null) {
                        IconButton(onClick = { purchaseViewModel.undoDelete() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig")
                        }
                    }
                    purchaseWithPositions?.purchase?.photoUri?.let { uriString ->
                        IconButton(onClick = {
                            val intent = Intent(context, PhotoViewActivity::class.java).apply {
                                putExtra("photo_uri", uriString)
                                putExtra("purchase_id", purchaseId)
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Photo, contentDescription = "Kassenbon anzeigen")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPositionDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Position hinzufügen")
            }
        }
    ) { innerPadding ->
        purchaseWithPositions?.let { purchaseData ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    PurchaseHeader(
                        purchase = purchaseData.purchase,
                        positionCount = purchaseData.positions.size,
                        onLongClick = { showEditPurchaseDialog = purchaseData.purchase }
                    )
                }
                items(purchaseData.positions, key = { it.id }) { position ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                purchaseViewModel.deletePosition(position)
                                true
                            } else {
                                false
                            }
                        },
                        positionalThreshold = { it * 0.90f } // Require 90% swipe
                    )

                    SwipeToDismissBox(
                        state = dismissState,
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
                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Icon",
                                        modifier = Modifier.scale(scale),
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        content = {
                            PositionDetailCard(position, onLongClick = {
                                showEditPositionDialog = it
                            })
                        }
                    )
                }
            }
        } ?: Column(modifier = Modifier.padding(innerPadding)) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PurchaseHeader(purchase: Purchase, positionCount: Int, onLongClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = dateFormatter.format(purchase.purchaseDate))
            Text(text = "$positionCount Pos")
            Text(text = "${"%.2f".format(purchase.totalPrice)} €")
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PositionDetailCard(position: Position, onLongClick: (Position) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(position) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Text(text = position.itemName, style = MaterialTheme.typography.titleMedium)
                Text(text = position.itemType, style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${position.quantity} ${position.unit}")
                Text(text = "@ ${"%.2f".format(position.unitPrice)} €", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "${"%.2f".format(position.price)} €",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PurchaseDetailScreenPreview() {
    CostManagerTheme {
        val previewPurchase = PurchaseWithPositions(
            purchase = Purchase(
                id = 1,
                purchaseDate = Date(),
                store = "Supermarkt A",
                totalPrice = 55.75,
                storeType = "Supermarkt"
            ),
            positions = listOf(
                Position(1, 1, "Milch", "Lebensmittel", 2.0, "Liter", 1.50, 3.00),
                Position(2, 1, "Brot", "Lebensmittel", 1.0, "Stück", 2.50, 2.50)
            )
        )
        // This preview won't work with the ViewModel fetching.
        // For a working preview, you'd pass the data directly.
    }
}