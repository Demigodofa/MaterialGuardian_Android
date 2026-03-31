package com.asme.receiving.ui

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asme.receiving.ui.MaterialGuardianColors
import java.io.File
import java.util.concurrent.Executor

private const val CAMERA_TAG = "MaterialGuardianCamera"

@Composable
fun CameraCaptureOverlay(
    title: String,
    maxCount: Int,
    currentCount: Int,
    captureIndex: Int?,
    onClose: () -> Unit,
    onCreateFile: (index: Int) -> File,
    onCaptureAccepted: (file: File, index: Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    var hasPermission by remember { mutableStateOf(false) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var pendingIndex by remember { mutableStateOf<Int?>(null) }
    var capturedPath by remember { mutableStateOf<String?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        hasPermission = isCameraPermissionGranted(context)
        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!hasPermission) {
            PermissionPrompt(onClose = onClose)
            return
        }

        val errorMessage = cameraError
        if (errorMessage != null) {
            CameraErrorPrompt(
                message = errorMessage,
                onClose = onClose,
                onDismiss = { cameraError = null }
            )
            return
        }

        if (capturedPath == null) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = imageCapture,
                lifecycleOwner = lifecycleOwner,
                context = context,
                onReady = {
                    cameraReady = true
                    cameraError = null
                },
                onError = { message ->
                    cameraReady = false
                    cameraError = message
                },
            )
        } else {
            val previewPath = capturedPath.orEmpty()
            val bitmap = decodeOrientedBitmap(previewPath)
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        val targetIndex = captureIndex ?: (currentCount + 1).coerceAtMost(maxCount)
        val canCapture = captureIndex != null || currentCount < maxCount

        CaptureOverlayControls(
            title = title,
            counter = formatCounter(targetIndex, maxCount),
            showShutter = capturedPath == null,
            canCapture = canCapture && cameraReady,
            onClose = onClose,
            onShutter = {
                if (!canCapture || !cameraReady) return@CaptureOverlayControls
                val file = onCreateFile(targetIndex)
                pendingFile = file
                pendingIndex = targetIndex
                takePicture(
                    executor = cameraExecutor,
                    imageCapture = imageCapture,
                    file = file,
                    onSaved = { path ->
                        capturedPath = path
                    },
                    onError = { message ->
                        pendingFile?.delete()
                        pendingFile = null
                        pendingIndex = null
                        cameraError = message
                    }
                )
            },
            onAccept = {
                val file = pendingFile
                val index = pendingIndex
                if (file != null && index != null) {
                    onCaptureAccepted(file, index)
                }
                capturedPath = null
            },
            onRetake = {
                pendingFile?.delete()
                pendingFile = null
                pendingIndex = null
                capturedPath = null
            },
        )
    }
}

@Composable
private fun PermissionPrompt(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Camera permission is required.",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun CameraErrorPrompt(
    message: String,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = "Camera unavailable",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Retry")
                }
                Button(onClick = onClose) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: Context,
    onReady: () -> Unit,
    onError: (String) -> Unit,
) {
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner, context, imageCapture) {
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    onReady()
                } catch (exception: Exception) {
                    Log.e(CAMERA_TAG, "Failed to bind camera preview", exception)
                    onError("The camera could not be started on this device right now.")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            runCatching {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { previewView },
    )
}

@Composable
private fun CaptureOverlayControls(
    title: String,
    counter: String,
    showShutter: Boolean,
    canCapture: Boolean,
    onClose: () -> Unit,
    onShutter: () -> Unit,
    onAccept: () -> Unit,
    onRetake: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onClose) {
                Text("Exit")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, color = Color.White)
                Text(text = counter, color = Color.White, modifier = Modifier.alpha(0.7f))
            }
            Spacer(modifier = Modifier.size(56.dp))
        }

        if (showShutter) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Button(
                    onClick = onShutter,
                    shape = CircleShape,
                    modifier = Modifier.size(92.dp),
                    enabled = canCapture,
                ) {
                    Text(text = "Capture")
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Retake")
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Accept")
                }
            }
        }
    }
}

private fun takePicture(
    executor: Executor,
    imageCapture: ImageCapture,
    file: File,
    onSaved: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(file.absolutePath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(CAMERA_TAG, "Failed to capture photo", exception)
                onError("The photo could not be captured. Please try again.")
            }
        },
    )
}

private fun isCameraPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun formatCounter(current: Int, max: Int): String {
    return "$current/$max"
}
