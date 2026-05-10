import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:boodschappen_app/main.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const BoodschappenApp());
    expect(find.byType(MaterialApp), findsOneWidget);
  });
}
