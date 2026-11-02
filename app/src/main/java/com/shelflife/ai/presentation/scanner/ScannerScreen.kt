package com.shelflife.ai.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.shelflife.ai.data.vision.CombinedVisionAnalyzer
import com.shelflife.ai.domain.model.Category
import com.shelflife.ai.presentation.components.ShelfLifeTopBar
import com.shelflife.ai.presentation.theme.BackgroundCanvas
import com.shelflife.ai.presentation.theme.EmeraldDark
import com.shelflife.ai.presentation.theme.EmeraldPrimary
import com.shelflife.ai.presentation.theme.EmeraldSoft
import com.shelflife.ai.presentation.theme.SlateMuted
import com.shelflife.ai.presentation.theme.SlateNavy
import com.shelflife.ai.presentation.theme.SlateText
import com.shelflife.ai.presentation.theme.SurfaceBorder
import com.shelflife.ai.presentation.theme.SurfaceSubtle
import com.shelflife.ai.presentation.theme.SurfaceWhite

/**
 * Replicated Live AR Scanner Screen matching live_ar_scanner/code.html:
 * [Top App Bar] -> [AR Viewport Frame with Radar Pulse & Laser Line] -> [Slide-Up Bottom Detection Sheet]
 */
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateToAddItem: (name: String?, barcode: String?, expiry: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }
    var previewViewInstance by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeFeedback()
        }
    }

    val combinedVisionAnalyzer = remember {
        CombinedVisionAnalyzer(onResult = { result ->
            viewModel.onScanResultReceived(result)
        })
    }

    DisposableEffect(combinedVisionAnalyzer) {
        onDispose {
            combinedVisionAnalyzer.close()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCanvas),
        containerColor = BackgroundCanvas,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShelfLifeTopBar(subtitle = "Scanner")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Simple, clean camera viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(24.dp))
            ) {
                if (hasCameraPermission) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        analyzer = combinedVisionAnalyzer,
                        isTorchEnabled = isTorchEnabled,
                        cameraSelector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA,
                        onPreviewViewReady = { previewViewInstance = it }
                    )

                    // Minimal Viewfinder Box (Corner brackets & subtle laser line)
                    ScannerCornerBrackets(
                        modifier = Modifier
                            .size(240.dp, 240.dp)
                            .align(Alignment.Center)
                    )

                    // Camera Top Bar: Status + Flash & Flip controls (Semi-transparent dark glass)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Scanner status badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary)
                                )
                                Text(
                                    text = if (!uiState.productName.isNullOrBlank()) "Item Found" else "Live",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        // Flash & Flip controls
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isTorchEnabled = !isTorchEnabled },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isTorchEnabled) EmeraldPrimary else Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Torch",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { isBackCamera = !isBackCamera },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraIos,
                                    contentDescription = "Flip Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Minimal Detection Floating Chip (Only appears when something is actively detected)
                    val detectedLabel = uiState.productName?.takeIf { it.isNotBlank() }
                        ?: uiState.scannedBarcode?.let { "Barcode: $it" }
                        ?: uiState.scannedRawText?.takeIf { it.isNotBlank() }

                    if (detectedLabel != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = detectedLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Camera Access Required",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ShelfLife AI uses real-time vision to detect expiry dates and barcodes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Enable Camera", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simplified Detection Action Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val hasDetection = !uiState.productName.isNullOrBlank() || !uiState.scannedBarcode.isNullOrBlank()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasDetection) "Scanned Item" else "Camera Ready",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (hasDetection) EmeraldDark else SlateMuted
                        )

                        TextButton(
                            onClick = {
                                onNavigateToAddItem(
                                    uiState.productName.takeIf { it.isNotBlank() },
                                    uiState.scannedBarcode,
                                    uiState.scannedExpiryTimestamp
                                )
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Manual Entry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    Text(
                        text = uiState.productName.takeIf { !it.isNullOrBlank() }
                            ?: uiState.scannedBarcode?.let { "Barcode: $it" }
                            ?: "Point camera at food item or barcode",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SlateNavy,
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val bmp = previewViewInstance?.bitmap
                                if (bmp != null) {
                                    viewModel.analyzeBitmapWithGemini(bmp)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Camera frame not ready yet. Please wait a second.")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SurfaceSubtle,
                                contentColor = SlateNavy
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                        ) {
                            if (uiState.isAnalyzingWithGemini) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyzing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Food Snap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                onNavigateToAddItem(
                                    uiState.productName.takeIf { it.isNotBlank() },
                                    uiState.scannedBarcode,
                                    uiState.scannedExpiryTimestamp
                                )
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (hasDetection) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasDetection) "Add Scanned Item" else "Enter Manually",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ScannerCornerBrackets(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laserBounce")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bracketLen = 22.dp.toPx()
        val strokeWidth = 2.5.dp.toPx()
        val color = EmeraldPrimary

        // Top-left
        drawLine(color, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth)
        drawLine(color, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth)

        // Top-right
        drawLine(color, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeWidth)
        drawLine(color, Offset(w, 0f), Offset(w, bracketLen), strokeWidth)

        // Bottom-left
        drawLine(color, Offset(0f, h), Offset(bracketLen, h), strokeWidth)
        drawLine(color, Offset(0f, h), Offset(0f, h - bracketLen), strokeWidth)

        // Bottom-right
        drawLine(color, Offset(w, h), Offset(w - bracketLen, h), strokeWidth)
        drawLine(color, Offset(w, h), Offset(w, h - bracketLen), strokeWidth)

        // Laser scan line
        val currentLaserY = h * laserY
        drawLine(
            color = EmeraldPrimary.copy(alpha = 0.85f),
            start = Offset(10.dp.toPx(), currentLaserY),
            end = Offset(w - 10.dp.toPx(), currentLaserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
