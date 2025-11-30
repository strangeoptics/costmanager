package com.example.costmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.costmanager.ui.theme.CostManagerTheme
import com.example.costmanager.ui.viewmodel.PurchaseViewModel

class PhotoViewActivity : ComponentActivity() {

    private val purchaseViewModel: PurchaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoUri = intent.getStringExtra("photo_uri")?.toUri()
        val purchaseId = intent.getLongExtra("purchase_id", -1L)
        if (photoUri == null || purchaseId == -1L) {
            finish()
            return
        }
        setContent {
            CostManagerTheme(darkTheme = true) {
                PhotoViewScreen(
                    photoUri = photoUri,
                    purchaseId = purchaseId,
                    purchaseViewModel = purchaseViewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewScreen(
    photoUri: Uri,
    purchaseId: Long,
    purchaseViewModel: PurchaseViewModel,
    onBack: () -> Unit
) {
    var currentUri by remember { mutableStateOf(photoUri) }
    val context = LocalContext.current

    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let {
                currentUri = it
                purchaseViewModel.updatePhotoUri(purchaseId, it.toString())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val cropOptions = CropImageContractOptions(
                            currentUri,
                            CropImageOptions()
                        )
                        cropImageLauncher.launch(cropOptions)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Photo")
                    }
                    IconButton(onClick = {
                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, currentUri)
                            type = "image/jpeg"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bild teilen"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Photo")
                    }
                }
            )
        }
    ) { paddingValues ->
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f) // Limit zoom level
            offset += offsetChange
        }
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .transformable(state = state)
        ) {
            AsyncImage(
                model = currentUri,
                contentDescription = "Kassenbon in voller Größe",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        }
    }
}
