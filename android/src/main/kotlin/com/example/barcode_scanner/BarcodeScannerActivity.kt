package com.example.barcode_scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class BarcodeScannerActivity : ComponentActivity() {

    companion object {
        private const val TAG = "BarcodeScannerActivity"
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Camera permission is required to scan barcodes",
                Toast.LENGTH_LONG
            ).show()
            returnError("Camera permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted, show UI
                setupComposeUI()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupComposeUI() {
        setContent {
            MaterialTheme {
                BarcodeScannerScreen(
                    onBarcodeDetected = { barcode ->
                        returnBarcode(barcode)
                    },
                    onError = { error ->
                        returnError(error)
                    }
                )
            }
        }
    }

    private fun returnBarcode(barcode: String) {
        val intent = Intent()
        intent.putExtra("barcode", barcode)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun returnError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasScanned by remember { mutableStateOf(false) }
    var scanningText by remember { mutableStateOf("Scanning...") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview
        CameraPreview(
            onBarcodeDetected = { barcode ->
                if (!hasScanned) {
                    hasScanned = true
                    onBarcodeDetected(barcode)
                }
            },
            onError = onError,
            lifecycleOwner = lifecycleOwner
        )

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Title Card
            Card(
                backgroundColor = Color.White.copy(alpha = 0.9f),
                elevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Barcode Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = scanningText,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Instructions
            Card(
                backgroundColor = Color.White.copy(alpha = 0.9f),
                elevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Position the barcode within the frame",
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Scanning Frame (visual guide)
        ScanningFrame(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun CameraPreview(
    onBarcodeDetected: (String) -> Unit,
    onError: (String) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                try {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview
                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }

                            // Image Analyzer
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                                        onBarcodeDetected(barcode)
                                    })
                                }

                            // Camera Selector
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            // Bind to lifecycle
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )

                            Log.d("CameraPreview", "Camera started successfully")

                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Camera binding failed", e)
                            onError("Camera initialization failed: ${e.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error starting camera", e)
                    onError("Camera start failed: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
fun ScanningFrame(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(280.dp, 280.dp)
    ) {
        // Top-left corner
        Canvas(modifier = Modifier
            .size(40.dp, 4.dp)
            .align(Alignment.TopStart)
        )
        Canvas(modifier = Modifier
            .size(4.dp, 40.dp)
            .align(Alignment.TopStart)
        )

        // Top-right corner
        Canvas(modifier = Modifier
            .size(40.dp, 4.dp)
            .align(Alignment.TopEnd)
        )
        Canvas(modifier = Modifier
            .size(4.dp, 40.dp)
            .align(Alignment.TopEnd)
        )

        // Bottom-left corner
        Canvas(modifier = Modifier
            .size(40.dp, 4.dp)
            .align(Alignment.BottomStart)
        )
        Canvas(modifier = Modifier
            .size(4.dp, 40.dp)
            .align(Alignment.BottomStart)
        )

        // Bottom-right corner
        Canvas(modifier = Modifier
            .size(40.dp, 4.dp)
            .align(Alignment.BottomEnd)
        )
        Canvas(modifier = Modifier
            .size(4.dp, 40.dp)
            .align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun Canvas(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White)
    )
}

// Barcode Analyzer (same as before)
private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            try {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                Log.d("BarcodeAnalyzer", "Barcode detected: $value")
                                onBarcodeDetected(value)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BarcodeAnalyzer", "Barcode scanning failed: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e("BarcodeAnalyzer", "Error analyzing image: ${e.message}", e)
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}