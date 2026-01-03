import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'barcode_scanner_platform_interface.dart';

/// An implementation of [BarcodeScannerPlatform] that uses method channels.
class MethodChannelBarcodeScanner extends BarcodeScannerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('barcode_scanner');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<String?> scanBarcode() async {
    final String? result = await methodChannel.invokeMethod<String>(
      'scanBarcode',
    );
    return result;
  }
}
