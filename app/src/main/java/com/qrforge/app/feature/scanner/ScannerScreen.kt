package com.qrforge.app.feature.scanner

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qrforge.app.core.scanner.CameraPermissionHandler
import com.qrforge.app.core.scanner.QRAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Gallery image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Run ML Kit on the picked image
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val scanner = BarcodeScanning.getClient(options)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val result = barcodes.firstOrNull()?.rawValue
                            if (result != null) {
                                viewModel.onQRDetected(result)
                            } else {
                                viewModel.onGalleryError("No QR code found in this image")
                            }
                        }
                        .addOnFailureListener {
                            viewModel.onGalleryError("Could not read image")
                        }
                }
            } catch (e: Exception) {
                viewModel.onGalleryError("Failed to open image")
            }
        }
    }

    CameraPermissionHandler {
        Box(modifier = Modifier.fillMaxSize()) {

            // Full screen camera preview
            CameraPreview(
                isFlashOn = uiState.isFlashOn,
                isScanning = uiState.isScanning,
                onQRDetected = { viewModel.onQRDetected(it) }
            )

            // Overlay with viewfinder
            ScannerOverlay()

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    "Scan QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = { viewModel.toggleFlash() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn
                        else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (uiState.isFlashOn) Color.Yellow else Color.White
                    )
                }
            }

            // Bottom controls — hint + gallery button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Point camera at any QR code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Gallery scan button
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Scan from Gallery")
                }
            }

            // Gallery error snackbar
            uiState.galleryError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.dismissGalleryError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Result Bottom Sheet
            if (uiState.showResultSheet) {
                uiState.scanResult?.let { result ->
                    ScanResultSheet(
                        result = result,
                        resultType = viewModel.getResultTypeDisplay(result),
                        copiedToClipboard = uiState.copiedToClipboard,
                        onCopy = { viewModel.copyToClipboard(context) },
                        onOpen = { viewModel.openUrl(context) },
                        onShare = { viewModel.shareResult(context) },
                        onDismiss = { viewModel.dismissResult() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    isFlashOn: Boolean,
    isScanning: Boolean,
    onQRDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var camera by remember { mutableStateOf<Camera?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(isFlashOn) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor, QRAnalyzer { result ->
                            if (isScanning) onQRDetected(result)
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun ScannerOverlay() {
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

    val viewfinderSize = 260.dp
    val cornerColor = Color(0xFF00E5A0)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .size(viewfinderSize)
                .align(Alignment.Center)
        ) {
            CornerBracket(Alignment.TopStart, cornerColor)
            CornerBracket(Alignment.TopEnd, cornerColor)
            CornerBracket(Alignment.BottomStart, cornerColor)
            CornerBracket(Alignment.BottomEnd, cornerColor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopStart)
                    .offset(y = viewfinderSize * scanLineY)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent, cornerColor, cornerColor, Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun BoxScope.CornerBracket(
    alignment: Alignment,
    color: Color,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    thickness: androidx.compose.ui.unit.Dp = 4.dp
) {
    val isTop   = alignment == Alignment.TopStart || alignment == Alignment.TopEnd
    val isStart = alignment == Alignment.TopStart || alignment == Alignment.BottomStart
    Box(modifier = Modifier.size(size).align(alignment)) {
        Box(modifier = Modifier.width(size).height(thickness).background(color)
            .align(if (isTop) Alignment.TopStart else Alignment.BottomStart))
        Box(modifier = Modifier.width(thickness).height(size).background(color)
            .align(if (isStart) Alignment.TopStart else Alignment.TopEnd))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanResultSheet(
    result: String,
    resultType: ScanResultType,
    copiedToClipboard: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(resultType.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    resultType.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (copiedToClipboard) "Copied!" else "Copy")
                }

                if (resultType == ScanResultType.URL) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Open", maxLines = 1, softWrap = false)
                    }
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", maxLines = 1, softWrap = false)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Scan Another Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}