#!/usr/bin/env python3
"""
upload_release.py — Upload APK naar Firebase Storage en update versie in Firestore.

Gebruik:
    python scripts/upload_release.py \
        --apk app/build/outputs/apk/release/BoodschappenlijstApp-v1.3.apk \
        --version-code 4 \
        --version-name "1.3" \
        --notes "Moderne UI, Firebase sync, dark/light theme"

Vereisten:
    pip install firebase-admin
    Zet GOOGLE_APPLICATION_CREDENTIALS naar je service account JSON, of geef --key-file mee.
"""

import argparse
import os
import sys
import datetime
import firebase_admin
from firebase_admin import credentials, storage, firestore


def main():
    parser = argparse.ArgumentParser(description="Upload APK naar Firebase en update versie")
    parser.add_argument("--apk",          required=True,  help="Pad naar APK bestand")
    parser.add_argument("--version-code", required=True,  type=int, help="versionCode (getal)")
    parser.add_argument("--version-name", required=True,  help="versionName (bijv. 1.3)")
    parser.add_argument("--notes",        default="",     help="Release notes (optioneel)")
    parser.add_argument("--mandatory",    action="store_true", help="Verplichte update")
    parser.add_argument("--key-file",     default=None,   help="Pad naar service account JSON")
    args = parser.parse_args()

    # ── Firebase initialiseren ────────────────────────────────────────────────
    key_file    = args.key_file or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    project_id  = "boodschappenlijst-app-claude"
    bucket_name = f"{project_id}.firebasestorage.app"

    if not key_file:
        print("❌ Geef --key-file mee of stel GOOGLE_APPLICATION_CREDENTIALS in")
        print("   Voorbeeld: --key-file pad/naar/service-account.json")
        sys.exit(1)

    if not os.path.exists(args.apk):
        print(f"❌ APK niet gevonden: {args.apk}")
        sys.exit(1)

    print(f"🔥 Firebase initialiseren...")
    cred = credentials.Certificate(key_file)
    firebase_admin.initialize_app(cred, {"storageBucket": bucket_name})

    # ── Upload naar Storage ───────────────────────────────────────────────────
    apk_filename    = os.path.basename(args.apk)
    storage_path    = f"releases/{apk_filename}"
    apk_size_mb     = os.path.getsize(args.apk) / (1024 * 1024)

    print(f"📤 Uploaden: {apk_filename} ({apk_size_mb:.1f} MB)...")

    bucket = storage.bucket()
    blob   = bucket.blob(storage_path)
    blob.upload_from_filename(args.apk, content_type="application/vnd.android.package-archive")

    # Publiek leesbaar maken
    blob.make_public()
    download_url = blob.public_url

    print(f"✅ Upload klaar!")
    print(f"🔗 Download URL: {download_url}")

    # ── Firestore versie document updaten ─────────────────────────────────────
    print(f"📝 Firestore bijwerken: v{args.version_name} (code {args.version_code})...")

    db = firestore.client()
    db.collection("app_updates").document("latest").set({
        "versionCode":  args.version_code,
        "versionName":  args.version_name,
        "downloadUrl":  download_url,
        "releaseNotes": args.notes,
        "mandatory":    args.mandatory,
        "uploadedAt":   datetime.datetime.now(datetime.timezone.utc),
        "fileName":     apk_filename,
        "sizeMb":       round(apk_size_mb, 1)
    })

    # Bewaar ook history
    db.collection("app_updates").document(f"v{args.version_name}").set({
        "versionCode":  args.version_code,
        "versionName":  args.version_name,
        "downloadUrl":  download_url,
        "releaseNotes": args.notes,
        "mandatory":    args.mandatory,
        "uploadedAt":   datetime.datetime.now(datetime.timezone.utc),
    })

    print(f"""
╔══════════════════════════════════════════════════════╗
║  ✅ Release v{args.version_name} succesvol gepubliceerd!
║
║  📦 APK:      {apk_filename[:48]}
║  🔢 Code:     {args.version_code}
║  ☁️  Storage:  {storage_path}
║  🔔 Gebruikers krijgen nu automatisch een update popup!
╚══════════════════════════════════════════════════════╝
""")


if __name__ == "__main__":
    main()
