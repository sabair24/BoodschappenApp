import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class ScannerScreen extends StatefulWidget {
  const ScannerScreen({super.key});

  @override
  State<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen> {
  final MobileScannerController _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
  );
  bool _scanned = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_scanned) return;
    final barcode = capture.barcodes.firstOrNull;
    if (barcode == null) return;
    final raw = barcode.rawValue;
    if (raw == null || raw.isEmpty) return;
    _scanned = true;
    Navigator.of(context).pop(raw);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        foregroundColor: Colors.white,
        title: const Text('Barcode scannen'),
        actions: [
          IconButton(
            icon: const Icon(Icons.flash_on),
            onPressed: () => _controller.toggleTorch(),
            tooltip: 'Zaklamp',
          ),
          IconButton(
            icon: const Icon(Icons.flip_camera_ios),
            onPressed: () => _controller.switchCamera(),
            tooltip: 'Camera omdraaien',
          ),
        ],
      ),
      body: Stack(
        children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
          ),
          IgnorePointer(
            child: CustomPaint(
              painter: _ScannerOverlayPainter(),
              child: const SizedBox.expand(),
            ),
          ),
          Positioned(
            bottom: 60,
            left: 0,
            right: 0,
            child: Center(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                decoration: BoxDecoration(
                  color: Colors.black54,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Text(
                  'Richt de camera op een barcode',
                  style: TextStyle(color: Colors.white, fontSize: 14),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ScannerOverlayPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = Colors.black.withOpacity(0.55);
    const cutoutWidth = 260.0;
    const cutoutHeight = 160.0;
    final left = (size.width - cutoutWidth) / 2;
    final top = (size.height - cutoutHeight) / 2;
    final rect = Rect.fromLTWH(left, top, cutoutWidth, cutoutHeight);

    final path = Path()
      ..addRect(Rect.fromLTWH(0, 0, size.width, size.height))
      ..addRRect(
          RRect.fromRectAndRadius(rect, const Radius.circular(12)))
      ..fillType = PathFillType.evenOdd;

    canvas.drawPath(path, paint);

    final cornerPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round;

    const cornerLen = 20.0;
    canvas.drawLine(Offset(left, top + cornerLen), Offset(left, top), cornerPaint);
    canvas.drawLine(Offset(left, top), Offset(left + cornerLen, top), cornerPaint);
    canvas.drawLine(Offset(left + cutoutWidth - cornerLen, top), Offset(left + cutoutWidth, top), cornerPaint);
    canvas.drawLine(Offset(left + cutoutWidth, top), Offset(left + cutoutWidth, top + cornerLen), cornerPaint);
    canvas.drawLine(Offset(left, top + cutoutHeight - cornerLen), Offset(left, top + cutoutHeight), cornerPaint);
    canvas.drawLine(Offset(left, top + cutoutHeight), Offset(left + cornerLen, top + cutoutHeight), cornerPaint);
    canvas.drawLine(Offset(left + cutoutWidth - cornerLen, top + cutoutHeight), Offset(left + cutoutWidth, top + cutoutHeight), cornerPaint);
    canvas.drawLine(Offset(left + cutoutWidth, top + cutoutHeight - cornerLen), Offset(left + cutoutWidth, top + cutoutHeight), cornerPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
