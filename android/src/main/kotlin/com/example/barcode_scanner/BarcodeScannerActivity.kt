package com.example.barcode_scanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerActivity : AppCompatActivity() {
    private var cameraExecutor: ExecutorService? = null
    private lateinit var previewView: PreviewView
    private var imageAnalyzer: ImageAnalysis? = null
    private var hasScanned = false

    companion object {
        private const val TAG = "BarcodeScannerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Create preview view
            previewView = PreviewView(this)
            setContentView(previewView)

            // Initialize camera executor
            cameraExecutor = Executors.newSingleThreadExecutor()

            // Check and request permissions
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                requestPermissions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            returnError("Initialization failed: ${e.message}")
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        try {
            // Ensure executor is initialized
            if (cameraExecutor == null) {
                cameraExecutor = Executors.newSingleThreadExecutor()
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    // Image analyzer
                    imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor!!, BarcodeAnalyzer { barcode ->
                                if (!hasScanned) {
                                    hasScanned = true
                                    runOnUiThread {
                                        returnBarcode(barcode)
                                    }
                                }
                            })
                        }

                    // Camera selector
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        // Unbind all use cases before rebinding
                        cameraProvider.unbindAll()

                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )

                        Log.d(TAG, "Camera started successfully")
                    } catch (exc: Exception) {
                        Log.e(TAG, "Use case binding failed", exc)
                        returnError("Camera binding failed: ${exc.message}")
                    }

                } catch (exc: Exception) {
                    Log.e(TAG, "Camera provider failed", exc)
                    returnError("Camera initialization failed: ${exc.message}")
                }
            }, ContextCompat.getMainExecutor(this))

        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera: ${e.message}", e)
            returnError("Camera start failed: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to scan barcodes",
                    Toast.LENGTH_LONG
                ).show()
                returnError("Camera permission denied")
            }
        }
    }

    private fun returnBarcode(barcode: String) {
        try {
            val intent = Intent()
            intent.putExtra("barcode", barcode)
            setResult(Activity.RESULT_OK, intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error returning barcode: ${e.message}", e)
            returnError("Error processing barcode: ${e.message}")
        }
    }

    private fun returnError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraExecutor?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera executor: ${e.message}", e)
        }
    }

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
                                    Log.d(TAG, "Barcode detected: $value")
                                    onBarcodeDetected(value)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Barcode scanning failed: ${e.message}", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error analyzing image: ${e.message}", e)
                    imageProxy.close()
                }
            } else {
                imageProxy.close()
            }
        }
    }
}