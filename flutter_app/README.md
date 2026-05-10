# Boodschappen App — Flutter

Flutter app for Dutch grocery shopping lists. Runs on **Android** and **iPad** from a single codebase.

## Features
- iPad split-view (two-pane layout on screens ≥768px)
- Barcode scanner via camera + Open Food Facts product lookup
- Shared lists via Firebase Firestore (same code system as the Android app)
- Auto-categorization of products (Dutch keyword matching)
- Dark/light theme, favorites, recent items, sort & filter
- Celebration animation when all items are checked off

## Setup

### 1. Install Flutter
```bash
flutter pub get
```

### 2. Firebase (for shared lists)
Add these files from your Firebase project:
- `android/app/google-services.json`
- `ios/Runner/GoogleService-Info.plist`

Then uncomment in `android/app/build.gradle`:
```groovy
id "com.google.gms.google-services"
```

### 3. Run
```bash
# Android
flutter run

# iPad (requires Mac + Xcode)
flutter run -d <ipad-device-id>
```

## Structure
```
lib/
  main.dart                    # App entry point
  models/                      # ShoppingItem, AppCategory
  data/                        # SQLite (local) + Firestore (shared)
  providers/shopping_provider.dart  # State management
  screens/                     # List, Add/Edit, Scanner
  widgets/                     # ItemCard, CategoryChips
  theme/app_theme.dart         # Material3 violet theme
  utils/category_utils.dart    # Auto-categorization logic
```
