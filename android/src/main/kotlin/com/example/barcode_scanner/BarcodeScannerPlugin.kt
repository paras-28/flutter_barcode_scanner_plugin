package com.example.barcode_scanner

import android.app.Activity
import android.content.Intent
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
        private const val BARCODE_SCAN_REQUEST = 100
    }

    /// for set up the connection between flutter and native android
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "barcode_scanner")
        channel.setMethodCallHandler(this)
    }

    /// Listens for messages from Flutter
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "scanBarcode") {
            if (activity == null) {
                result.error("NO_ACTIVITY", "Plugin not attached to an activity", null)
                return
            }

            pendingResult = result
            val intent = Intent(activity, BarcodeScannerActivity::class.java)
            activity?.startActivityForResult(intent, BARCODE_SCAN_REQUEST)
        } else if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else {
            result.notImplemented()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == BARCODE_SCAN_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val barcode = data.getStringExtra("barcode")
                pendingResult?.success(barcode)
            } else {
                pendingResult?.success(null)
            }
            pendingResult = null
            return true
        }
        return false
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    /// reconnect
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}


/** BarcodeScannerPlugin */

/*
class BarcodeScannerPlugin :
    FlutterPlugin,
    MethodCallHandler {
    // The MethodChannel that will the communication between Flutter and native Android
    //
    // This local reference serves to register the plugin with the Flutter Engine and unregister it
    // when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    /// for set up the connection between flutter and native android
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "barcode_scanner")
        channel.setMethodCallHandler(this)
    }

    /// Listens for messages from Flutter
    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else {
            result.notImplemented()
        }
    }

    /// When the app closes, it disconnects the phone line to avoid memory leaks
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}

*/


