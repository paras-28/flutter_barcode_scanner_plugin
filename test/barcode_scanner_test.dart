import 'package:flutter_test/flutter_test.dart';
import 'package:barcode_scanner/barcode_scanner.dart';
import 'package:barcode_scanner/barcode_scanner_platform_interface.dart';
import 'package:barcode_scanner/barcode_scanner_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockBarcodeScannerPlatform
    with MockPlatformInterfaceMixin
    implements BarcodeScannerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<String?> scanBarcode() {
    // TODO: implement scanBarcode
    throw UnimplementedError();
  }
}

void main() {
  final BarcodeScannerPlatform initialPlatform = BarcodeScannerPlatform.instance;

  test('$MethodChannelBarcodeScanner is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelBarcodeScanner>());
  });

  test('getPlatformVersion', () async {
    BarcodeScanner barcodeScannerPlugin = BarcodeScanner();
    MockBarcodeScannerPlatform fakePlatform = MockBarcodeScannerPlatform();
    BarcodeScannerPlatform.instance = fakePlatform;

    expect(await barcodeScannerPlugin.getPlatformVersion(), '42');
  });
}
