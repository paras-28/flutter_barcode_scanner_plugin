
import 'barcode_scanner_platform_interface.dart';

class BarcodeScanner {
  Future<String?> getPlatformVersion() {
    return BarcodeScannerPlatform.instance.getPlatformVersion();
  }

  Future<String?> scanBarcode() {
    return BarcodeScannerPlatform.instance.scanBarcode();
  }
}
