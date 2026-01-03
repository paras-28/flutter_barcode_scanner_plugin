package com.example.barcode_scanner

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

class BarcodeScannerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var pendingResult: Result? = null

    companion object {
        private const val TAG = "BarcodeScannerPlugin"
        private const val CHANNEL_NAME = "barcode_scanner"
        private const val BARCODE_SCAN_REQUEST = 100
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Plugin attached to engine")
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        Log.d(TAG, "Method called: ${call.method}")

        when (call.method) {
            "scanBarcode" -> {
                if (activity == null) {
                    Log.e(TAG, "Activity is null")
                    result.error("NO_ACTIVITY", "Plugin not attached to an activity", null)
                    return
                }

                if (pendingResult != null) {
                    Log.w(TAG, "Scan already in progress")
                    result.error("ALREADY_ACTIVE", "A barcode scan is already in progress", null)
                    return
                }

                try {
                    pendingResult = result
                    val intent = Intent(activity, BarcodeScannerActivity::class.java)
                    activity?.startActivityForResult(intent, BARCODE_SCAN_REQUEST)
                    Log.d(TAG, "Scanner activity started")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting scanner: ${e.message}", e)
                    pendingResult = null
                    result.error("START_FAILED", "Failed to start scanner: ${e.message}", null)
                }
            }

            "getPlatformVersion" -> {
                val version = "Android ${android.os.Build.VERSION.RELEASE}"
                Log.d(TAG, "Platform version: $version")
                result.success(version)
            }

            else -> {
                Log.w(TAG, "Method not implemented: ${call.method}")
                result.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        Log.d(TAG, "Activity result received - requestCode: $requestCode, resultCode: $resultCode")

        if (requestCode == BARCODE_SCAN_REQUEST) {
            val result = pendingResult
            pendingResult = null

            if (result == null) {
                Log.w(TAG, "No pending result found")
                return false
            }

            when (resultCode) {
                Activity.RESULT_OK -> {
                    val barcode = data?.getStringExtra("barcode")
                    Log.d(TAG, "Barcode scanned: $barcode")
                    result.success(barcode)
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Scan cancelled")
                    result.success(null)
                }
                else -> {
                    Log.w(TAG, "Unexpected result code: $resultCode")
                    result.success(null)
                }
            }
            return true
        }

        return false
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(TAG, "Plugin detached from engine")
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(TAG, "Plugin attached to activity")
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(TAG, "Plugin detached from activity for config changes")
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(TAG, "Plugin reattached to activity for config changes")
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        Log.d(TAG, "Plugin detached from activity")
        activity = null
    }
}