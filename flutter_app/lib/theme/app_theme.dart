import 'package:flutter/material.dart';

class AppTheme {
  static ThemeData lightTheme() {
    const seedColor = Color(0xFF7C3AED); // Violet-600
    final cs = ColorScheme.fromSeed(
      seedColor: seedColor,
      brightness: Brightness.light,
      primary: const Color(0xFF7C3AED),
      secondary: const Color(0xFF9F67FA),
      surface: const Color(0xFFF8F5FF),
      onSurface: const Color(0xFF1A0A3C),
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: cs,
      scaffoldBackgroundColor: const Color(0xFFF3EEFF),
      appBarTheme: AppBarTheme(
        backgroundColor: const Color(0xFF7C3AED),
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: const TextStyle(
          color: Colors.white,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
        iconTheme: const IconThemeData(color: Colors.white),
        actionsIconTheme: const IconThemeData(color: Colors.white),
      ),
      cardTheme: CardThemeData(
        elevation: 2,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        color: Colors.white,
      ),
      chipTheme: ChipThemeData(
        selectedColor: const Color(0xFF7C3AED),
        labelStyle: const TextStyle(fontSize: 13),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: Color(0xFF7C3AED),
        foregroundColor: Colors.white,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFD8C8FF)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFD8C8FF)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF7C3AED), width: 2),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFF7C3AED),
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: Color(0xFF7C3AED),
        linearTrackColor: Color(0xFFD8C8FF),
      ),
    );
  }

  static ThemeData darkTheme() {
    const seedColor = Color(0xFF9F67FA); // Violet-400
    final cs = ColorScheme.fromSeed(
      seedColor: seedColor,
      brightness: Brightness.dark,
      primary: const Color(0xFF9F67FA),
      secondary: const Color(0xFFBB86FC),
      surface: const Color(0xFF1A0A3C),
      onSurface: const Color(0xFFEDE0FF),
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: cs,
      scaffoldBackgroundColor: const Color(0xFF120830),
      appBarTheme: AppBarTheme(
        backgroundColor: const Color(0xFF1E0C44),
        foregroundColor: const Color(0xFFEDE0FF),
        elevation: 0,
        centerTitle: false,
        titleTextStyle: const TextStyle(
          color: Color(0xFFEDE0FF),
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
        iconTheme: const IconThemeData(color: Color(0xFFEDE0FF)),
        actionsIconTheme: const IconThemeData(color: Color(0xFFEDE0FF)),
      ),
      cardTheme: CardThemeData(
        elevation: 4,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        color: const Color(0xFF2A1050),
      ),
      chipTheme: ChipThemeData(
        selectedColor: const Color(0xFF9F67FA),
        labelStyle: const TextStyle(fontSize: 13),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: Color(0xFF9F67FA),
        foregroundColor: Colors.white,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF2A1050),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF4A2480)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF4A2480)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF9F67FA), width: 2),
        ),
        labelStyle: const TextStyle(color: Color(0xFFBB86FC)),
        hintStyle: TextStyle(color: Colors.white.withOpacity(0.4)),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFF9F67FA),
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        ),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: Color(0xFF9F67FA),
        linearTrackColor: Color(0xFF2A1050),
      ),
      dividerTheme: const DividerThemeData(color: Color(0xFF3A1A60)),
    );
  }

  static LinearGradient gradientBackground(bool isDark) {
    if (isDark) {
      return const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFF1A0A3C), Color(0xFF0D0520), Color(0xFF120830)],
      );
    } else {
      return const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFFF3EEFF), Color(0xFFEDE0FF), Color(0xFFFFFFFF)],
      );
    }
  }

  static Color categoryColor(String categoryName, bool isDark) {
    final colors = isDark ? _darkCategoryColors : _lightCategoryColors;
    return colors[categoryName] ?? (isDark ? const Color(0xFF9E9E9E) : const Color(0xFF757575));
  }

  static const Map<String, Color> _lightCategoryColors = {
    'Groente & Fruit': Color(0xFF4CAF50),
    'Zuivel': Color(0xFF2196F3),
    'Vlees & Vis': Color(0xFFFF5722),
    'Bakkerij': Color(0xFFFF9800),
    'Dranken': Color(0xFF00BCD4),
    'Diepvries': Color(0xFF3F51B5),
    'Snoep & Koek': Color(0xFFE91E63),
    'Verzorging': Color(0xFF9C27B0),
    'Huishouden': Color(0xFF607D8B),
    'Overig': Color(0xFF795548),
  };

  static const Map<String, Color> _darkCategoryColors = {
    'Groente & Fruit': Color(0xFF66BB6A),
    'Zuivel': Color(0xFF42A5F5),
    'Vlees & Vis': Color(0xFFFF7043),
    'Bakkerij': Color(0xFFFFA726),
    'Dranken': Color(0xFF26C6DA),
    'Diepvries': Color(0xFF5C6BC0),
    'Snoep & Koek': Color(0xFFEC407A),
    'Verzorging': Color(0xFFAB47BC),
    'Huishouden': Color(0xFF78909C),
    'Overig': Color(0xFF8D6E63),
  };
}
