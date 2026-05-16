package com.boodschappen.app.ui.screens

import android.util.Base64
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.boodschappen.app.data.repository.ReceiptItem
import com.boodschappen.app.ui.theme.*
import com.boodschappen.app.viewmodel.ReceiptScanState
import com.boodschappen.app.viewmodel.ShoppingViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerScreen(
    viewModel: ShoppingViewModel,
    onNavigateBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val receiptScanState by viewModel.receiptScanState.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    var selectedItems by remember { mutableStateOf(setOf<Int>()) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetReceiptScanState()
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(receiptScanState) {
        if (receiptScanState is ReceiptScanState.Ready) {
            val items = (receiptScanState as ReceiptScanState.Ready).items
            selectedItems = items.indices.toSet()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetReceiptScanState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("📄 Kassabon scannen", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Terug", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermission.status.isGranted -> {
                    PermissionDeniedContent(
                        shouldShowRationale = cameraPermission.status.shouldShowRationale,
                        onRequestPermission = { cameraPermission.launchPermissionRequest() },
                        onNavigateBack = onNavigateBack
                    )
                }

                receiptScanState is ReceiptScanState.Ready -> {
                    val items = (receiptScanState as ReceiptScanState.Ready).items
                    ResultPanel(
                        items = items,
                        selectedItems = selectedItems,
                        isDark = isDark,
                        onToggle = { idx ->
                            selectedItems = if (idx in selectedItems) selectedItems - idx else selectedItems + idx
                        },
                        onAdd = {
                            val toAdd = items.filterIndexed { i, _ -> i in selectedItems }
                            viewModel.addReceiptItems(toAdd)
                            onNavigateBack()
                        },
                        onRetry = {
                            viewModel.resetReceiptScanState()
                            isCapturing = false
                        }
                    )
                }

                else -> {
                    val context = LocalContext.current
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, capture
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("ReceiptScanner", "Camera bind failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (receiptScanState) {
                            is ReceiptScanState.Analyzing -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(0.7f))
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("AI analyseert de bon...", color = Color.White)
                                    }
                                }
                            }
                            is ReceiptScanState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Red.copy(0.7f))
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        (receiptScanState as ReceiptScanState.Error).message,
                                        color = Color.White, textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                ShutterButton(
                                    isCapturing = false,
                                    onClick = { capturePhoto(context, imageCapture, cameraExecutor, isCapturing, viewModel) { isCapturing = it } }
                                )
                            }
                            else -> {
                                Text(
                                    "Richt de camera op je kassabon\nen druk op de knop",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(0.5f))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                Spacer(Modifier.height(24.dp))
                                ShutterButton(
                                    isCapturing = isCapturing,
                                    onClick = { capturePhoto(context, imageCapture, cameraExecutor, isCapturing, viewModel) { isCapturing = it } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun capturePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.ExecutorService,
    isCapturing: Boolean,
    viewModel: ShoppingViewModel,
    setCapturing: (Boolean) -> Unit
) {
    if (isCapturing || imageCapture == null) return
    setCapturing(true)
    val tempFile = File.createTempFile("receipt_", ".jpg", context.cacheDir)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
    imageCapture.takePicture(outputOptions, executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bytes = tempFile.readBytes()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    viewModel.scanReceipt(base64)
                } finally {
                    tempFile.delete()
                    setCapturing(false)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("ReceiptScanner", "Capture failed", exception)
                setCapturing(false)
            }
        }
    )
}

@Composable
private fun PermissionDeniedContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📷", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            if (shouldShowRationale) "Cameratoegang nodig om kassabonnen te scannen"
            else "Cameratoegang geweigerd. Geef toegang via instellingen.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        if (shouldShowRationale) {
            Button(onClick = onRequestPermission) { Text("Toestemming geven") }
            Spacer(Modifier.height(8.dp))
        }
        TextButton(onClick = onNavigateBack) {
            Text("Terug", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isCapturing) 0.5f else 1f))
            .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(3.dp, Color.Black, CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ResultPanel(
    items: List<ReceiptItem>,
    selectedItems: Set<Int>,
    isDark: Boolean,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Dark900 else Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Gevonden producten", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1A1040))
                Text("${selectedItems.size} van ${items.size} geselecteerd",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Opnieuw")
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size) { idx ->
                val item = items[idx]
                val isSelected = idx in selectedItems
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected)
                                if (isDark) Violet80.copy(0.15f) else Violet40.copy(0.08f)
                            else if (isDark) Dark700 else Color(0xFFF8F8FF)
                        )
                        .border(
                            1.dp,
                            if (isSelected) (if (isDark) Violet80 else Violet40).copy(0.4f)
                            else if (isDark) Dark600 else Color(0xFFE0D9FF),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggle(idx) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle(idx) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isDark) Violet80 else Violet40
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White else Color(0xFF1A1040))
                        if (item.quantity != "1") {
                            Text("Aantal: ${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (item.price != null) {
                        Text(
                            "€${"%.2f".format(item.price).replace('.', ',')}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Emerald80 else Emerald40
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selectedItems.isNotEmpty())
                        Brush.linearGradient(listOf(
                            if (isDark) Violet80 else Violet40,
                            if (isDark) Emerald80 else Emerald40
                        ))
                    else Brush.linearGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.3f)))
                )
                .clickable(enabled = selectedItems.isNotEmpty(), onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Voeg ${selectedItems.size} producten toe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedItems.isNotEmpty()) Color.White else Color.Gray
                )
            }
        }
    }
}
