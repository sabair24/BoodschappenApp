# 🛒 Boodschappen App

Een moderne Android boodschappenlijst app met barcode scanner.

## Functies
- ✅ Items toevoegen, bewerken, verwijderen
- 📦 Categorieën (Groente, Zuivel, Vlees, etc.)
- 📷 Barcode scanner (EAN-8, EAN-13, UPC-A/E)
- 🌐 Automatisch product herkennen via Open Food Facts
- 🖼️ Product afbeeldingen ophalen
- 📤 Lijst delen via WhatsApp, email, etc.
- ✓ Items afstrepen met voortgangsbalk
- 🔍 Filter op categorie
- 💾 Offline opslaan (Room database)

## Bouwen (Android Studio)

1. Download en installeer **Android Studio** (gratis): https://developer.android.com/studio
2. Open Android Studio → "Open an existing project"
3. Selecteer de map `/Users/saberlajili/Downloads/BoodschappenApp`
4. Wacht tot Gradle sync klaar is (eerste keer ~5 min)
5. Sluit je Android telefoon aan via USB (of gebruik de emulator)
6. Klik op de groene ▶ Play knop

## APK bouwen zonder telefoon (installeerbaar bestand)

In Android Studio:
- Bovenste menu → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
- De APK staat dan in: `app/build/outputs/apk/debug/app-debug.apk`
- Stuur dit bestand naar je telefoon en installeer het

## Vereisten
- Android 8.0 (API 26) of hoger
- Camera voor barcode scanner
- Internet voor product lookup

## Tech Stack
- Kotlin + Jetpack Compose (Material Design 3)
- Room (lokale database)
- CameraX + ML Kit (barcode scanner)
- Retrofit + Open Food Facts API
- Coil (afbeeldingen laden)
