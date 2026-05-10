// NOTE: Add google-services.json to android/app/ and GoogleService-Info.plist
// to ios/Runner/ before enabling Firebase features.

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:firebase_core/firebase_core.dart';
import 'providers/shopping_provider.dart';
import 'screens/shopping_list_screen.dart';
import 'theme/app_theme.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Firebase
  try {
    await Firebase.initializeApp();
  } catch (e) {
    // Firebase not configured yet — app runs in local-only mode.
    debugPrint('Firebase init skipped: $e');
  }

  runApp(
    ChangeNotifierProvider(
      create: (_) => ShoppingProvider()..init(),
      child: const BoodschappenApp(),
    ),
  );
}

class BoodschappenApp extends StatelessWidget {
  const BoodschappenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ShoppingProvider>(
      builder: (context, provider, _) {
        return MaterialApp(
          title: 'Boodschappen',
          debugShowCheckedModeBanner: false,
          themeMode: provider.isDarkTheme ? ThemeMode.dark : ThemeMode.light,
          theme: AppTheme.lightTheme(),
          darkTheme: AppTheme.darkTheme(),
          home: const ShoppingListScreen(),
        );
      },
    );
  }
}
