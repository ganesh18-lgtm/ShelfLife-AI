package com.shelflife.ai.presentation.scanner

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "CameraPreview"

/**
 * Jetpack Compose host for CameraX live camera preview and frame analysis.
 *
 * @param modifier Compose layout modifier applied to the preview container.
 * @param analyzer Optional [ImageAnalysis.Analyzer] (e.g. CombinedVisionAnalyzer).
 * @param onPreviewViewReady Optional callback delivering the underlying [PreviewView] instance.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: ImageAnalysis.Analyzer? = null,
    isTorchEnabled: Boolean = false,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onPreviewViewReady: ((PreviewView) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Background executor dedicated to ML Kit image analysis
    val cameraExecutor: ExecutorService = remember(lifecycleOwner) {
        Executors.newSingleThreadExecutor()
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val currentAnalyzer = rememberUpdatedState(analyzer)

    // Bind CameraX use cases whenever PreviewView, selector, or torch changes
    LaunchedEffect(previewView, lifecycleOwner, cameraSelector, isTorchEnabled) {
        val pv = previewView ?: return@LaunchedEffect
        val activeAnalyzer = currentAnalyzer.value

        try {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(pv.surfaceProvider)
                }

            val camera = if (activeAnalyzer != null) {
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, activeAnalyzer)
                    }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } else {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            }

            try {
                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(isTorchEnabled)
                }
            } catch (te: Throwable) {
                Log.w(TAG, "Flash/torch toggle handled safely: ${te.message}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to bind CameraX use cases", e)
        }
    }

    // Teardown: Unbind camera use cases and terminate background executor on disposal
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val future = ProcessCameraProvider.getInstance(context)
                if (future.isDone) {
                    future.get().unbindAll()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error unbinding CameraX on disposal", e)
            }
            try {
                cameraExecutor.shutdown()
            } catch (_: Throwable) {}
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }.also { pv ->
                previewView = pv
                onPreviewViewReady?.invoke(pv)
            }
        }
    )
}

/**
 * Asynchronously retrieves [ProcessCameraProvider] wrapped in a coroutine.
 */
private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }
