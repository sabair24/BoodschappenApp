package com.boodschappen.app.ui.screens

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.boodschappen.app.data.local.Category
import com.boodschappen.app.data.local.ShoppingItem
import com.boodschappen.app.viewmodel.ScanState
import com.boodschappen.app.viewmodel.ShoppingViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ShoppingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val scanState by viewModel.scanState.collectAsState()
    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.resetScanState()
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Scannen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetScanState()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Terug")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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

                else -> {
                    CameraPreviewWithScanner(
                        isActive = isScanning && scanState is ScanState.Idle,
                        onBarcodeDetected = { barcode ->
                            if (barcode != lastScannedBarcode && scanState is ScanState.Idle) {
                                lastScannedBarcode = barcode
                                isScanning = false
                                viewModel.lookupBarcode(barcode)
                            }
                        }
                    )

                    // Scanner overlay
                    ScannerOverlay()

                    // Bottom sheet with results
                    AnimatedVisibility(
                        visible = scanState !is ScanState.Idle,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        when (val state = scanState) {
                            is ScanState.Scanning -> {
                                ScanResultCard {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Product opzoeken...", color = Color.White)
                                }
                            }

                            is ScanState.Found -> {
                                val product = state.product
                                ProductFoundCard(
                                    productName = product.getBestName() ?: "Onbekend product",
                                    brand = product.brands?.split(",")?.firstOrNull()?.trim(),
                                    imageUrl = product.getBestImage(),
                                    barcode = state.barcode,
                                    onAddToList = {
                                        onNavigateToAdd()
                                    },
                                    onAddDirectly = {
                                        viewModel.addItem(
                                            ShoppingItem(
                                                name = product.getBestName() ?: "Onbekend",
                                                brand = product.brands?.split(",")?.firstOrNull()?.trim(),
                                                imageUrl = product.getBestImage(),
                                                barcode = state.barcode,
                                                quantity = "1",
                                                category = Category.OVERIG.displayName
                                            )
                                        )
                                        viewModel.resetScanState()
                                        isScanning = true
                                        lastScannedBarcode = null
                                    },
                                    onDismiss = {
                                        viewModel.resetScanState()
                                        isScanning = true
                                        lastScannedBarcode = null
                                    }
                                )
                            }

                            is ScanState.NotFound -> {
                                ProductNotFoundCard(
                                    barcode = state.barcode,
                                    error = state.error,
                                    onManualAdd = { onNavigateToAdd() },
                                    onRetry = {
                                        viewModel.resetScanState()
                                        isScanning = true
                                        lastScannedBarcode = null
                                    }
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithScanner(
    isActive: Boolean,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (!isActive) {
                                imageProxy.close()
                                return@setAnalyzer
                            }
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull { it.format in listOf(
                                            Barcode.FORMAT_EAN_13,
                                            Barcode.FORMAT_EAN_8,
                                            Barcode.FORMAT_UPC_A,
                                            Barcode.FORMAT_UPC_E,
                                            Barcode.FORMAT_QR_CODE,
                                            Barcode.FORMAT_CODE_128
                                        ) }?.rawValue?.let { barcode ->
                                            onBarcodeDetected(barcode)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("Scanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val overlayColor = Color.Black.copy(alpha = 0.6f)
        val frameSize = minOf(size.width, size.height) * 0.72f
        val left = (size.width - frameSize) / 2
        val top = (size.height - frameSize) / 2 - 60.dp.toPx()
        val right = left + frameSize
        val bottom = top + frameSize
        val cornerLength = 50.dp.toPx()
        val cornerRadius = 12.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // Dark overlay (4 rectangles around the frame)
        drawRect(overlayColor, size = androidx.compose.ui.geometry.Size(size.width, top))
        drawRect(overlayColor, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, frameSize))
        drawRect(overlayColor, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(size.width - right, frameSize))
        drawRect(overlayColor, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(size.width, size.height - bottom))

        val cornerColor = Color(0xFF4CAF50)

        // Top-left corner
        drawLine(cornerColor, Offset(left, top + cornerRadius), Offset(left, top + cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(cornerColor, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)

        // Top-right corner
        drawLine(cornerColor, Offset(right, top + cornerRadius), Offset(right, top + cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(cornerColor, Offset(right - cornerRadius, top), Offset(right - cornerLength, top), strokeWidth, StrokeCap.Round)

        // Bottom-left corner
        drawLine(cornerColor, Offset(left, bottom - cornerRadius), Offset(left, bottom - cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(cornerColor, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth, StrokeCap.Round)

        // Bottom-right corner
        drawLine(cornerColor, Offset(right, bottom - cornerRadius), Offset(right, bottom - cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(cornerColor, Offset(right - cornerRadius, bottom), Offset(right - cornerLength, bottom), strokeWidth, StrokeCap.Round)

        // Scan line
        val lineY = top + frameSize * scanLineY
        drawLine(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFF4CAF50), Color(0xFF4CAF50), Color.Transparent),
                startX = left,
                endX = right
            ),
            start = Offset(left + 16.dp.toPx(), lineY),
            end = Offset(right - 16.dp.toPx(), lineY),
            strokeWidth = 2.dp.toPx()
        )
    }

    Box(Modifier.fillMaxSize()) {
        Text(
            "Richt de camera op een barcode",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 240.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ScanResultCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun ProductFoundCard(
    productName: String,
    brand: String?,
    imageUrl: String?,
    barcode: String,
    onAddToList: () -> Unit,
    onAddDirectly: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(Modifier.padding(20.dp)) {
            // Drag handle
            Box(
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = productName,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                } else {
                    Box(
                        Modifier
                            .size(80.dp)
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("🛒", fontSize = 32.sp) }
                    Spacer(Modifier.width(16.dp))
                }

                Column(Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF34C759).copy(alpha = 0.2f)
                    ) {
                        Text(
                            "✓ Gevonden",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34C759),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (brand != null) {
                        Text(
                            brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        barcode,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Opnieuw", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onAddDirectly,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759),
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Direct", fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onAddToList,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text("Bewerken", fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ProductNotFoundCard(
    barcode: String,
    error: String? = null,
    onManualAdd: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Product niet gevonden",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Barcode: $barcode",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6B6B).copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                ) {
                    Text("Opnieuw")
                }
                Button(
                    onClick = onManualAdd,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF))
                ) {
                    Text("Handmatig")
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedContent(
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
        Text("📷", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera toegang nodig",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (shouldShowRationale)
                "De camera is nodig om barcodes te scannen. Geef toegang in je instellingen."
            else
                "De app heeft camera toegang nodig om barcodes te scannen.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Toegang geven")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Terug", color = Color.Gray)
        }
    }
}
