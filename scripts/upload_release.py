#!/usr/bin/env python3
"""
upload_release.py — Upload APK naar GitHub Releases en update versie in Firestore.

Gebruik:
    python scripts/upload_release.py \
        --apk app/build/outputs/apk/release/BoodschappenlijstApp-v1.4.apk \
        --version-code 5 \
        --version-name "1.4" \
        --notes "Moderne UI, Firebase sync, dark/light theme, in-app updates" \
        --github-token YOUR_GITHUB_TOKEN \
        --key-file pad/naar/service-account.json

Vereisten:
    pip install firebase-admin requests
    GitHub token: https://github.com/settings/tokens (scope: repo)
    Firestore service account JSON of GOOGLE_APPLICATION_CREDENTIALS env var
"""

import argparse
import os
import sys
import datetime
import json
import requests
import firebase_admin
from firebase_admin import credentials, firestore

GITHUB_REPO = "sabair24/BoodschappenApp"
GITHUB_API  = "https://api.github.com"


def create_github_release(token: str, tag: str, version_name: str, notes: str) -> dict:
    """Maak een GitHub Release aan en geef het release object terug."""
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    payload = {
        "tag_name":         f"v{version_name}",
        "target_commitish": "main",
        "name":             f"v{version_name}",
        "body":             notes or f"Release v{version_name}",
        "draft":            False,
        "prerelease":       False,
    }
    r = requests.post(
        f"{GITHUB_API}/repos/{GITHUB_REPO}/releases",
        headers=headers,
        json=payload,
        timeout=30,
    )
    if r.status_code == 422:
        # Release bestaat al — haal bestaande op
        print(f"⚠️  Release v{version_name} bestaat al, gebruik bestaande release.")
        r2 = requests.get(
            f"{GITHUB_API}/repos/{GITHUB_REPO}/releases/tags/v{version_name}",
            headers=headers,
            timeout=30,
        )
        r2.raise_for_status()
        return r2.json()
    r.raise_for_status()
    return r.json()


def upload_apk_to_release(token: str, release: dict, apk_path: str) -> str:
    """Upload APK als asset aan de GitHub Release, geef download URL terug."""
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
        "Content-Type": "application/vnd.android.package-archive",
    }
    apk_name     = os.path.basename(apk_path)
    upload_url   = release["upload_url"].replace("{?name,label}", "")
    apk_size_mb  = os.path.getsize(apk_path) / (1024 * 1024)

    print(f"📤 Uploaden naar GitHub: {apk_name} ({apk_size_mb:.1f} MB)...")

    # Verwijder bestaand asset met dezelfde naam als dat bestaat
    existing_assets = release.get("assets", [])
    for asset in existing_assets:
        if asset["name"] == apk_name:
            print(f"   ♻️  Verwijder oud asset: {apk_name}")
            requests.delete(
                f"{GITHUB_API}/repos/{GITHUB_REPO}/releases/assets/{asset['id']}",
                headers=headers,
                timeout=30,
            )

    with open(apk_path, "rb") as f:
        r = requests.post(
            f"{upload_url}?name={apk_name}",
            headers=headers,
            data=f,
            timeout=300,
        )
    r.raise_for_status()

    asset        = r.json()
    download_url = asset["browser_download_url"]
    print(f"✅ Upload klaar!")
    print(f"🔗 Download URL: {download_url}")
    return download_url


def update_firestore(key_file: str, version_code: int, version_name: str,
                     download_url: str, notes: str, mandatory: bool,
                     apk_path: str) -> None:
    """Update app_updates/latest en sla versiegeschiedenis op in Firestore."""
    project_id = "boodschappenlijst-app-claude"

    print(f"🔥 Firebase initialiseren...")
    cred = credentials.Certificate(key_file)
    # Vermijd dubbele initialisatie bij hergebruik
    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {"projectId": project_id})

    apk_name    = os.path.basename(apk_path)
    apk_size_mb = round(os.path.getsize(apk_path) / (1024 * 1024), 1)
    now         = datetime.datetime.now(datetime.timezone.utc)

    doc = {
        "versionCode":  version_code,
        "versionName":  version_name,
        "downloadUrl":  download_url,
        "releaseNotes": notes,
        "mandatory":    mandatory,
        "uploadedAt":   now,
        "fileName":     apk_name,
        "sizeMb":       apk_size_mb,
    }

    db = firestore.client()
    print(f"📝 Firestore bijwerken: v{version_name} (code {version_code})...")
    db.collection("app_updates").document("latest").set(doc)
    db.collection("app_updates").document(f"v{version_name}").set(doc)
    print("✅ Firestore bijgewerkt!")


def main():
    parser = argparse.ArgumentParser(description="Upload APK naar GitHub Releases en update Firestore")
    parser.add_argument("--apk",            required=True,       help="Pad naar APK bestand")
    parser.add_argument("--version-code",   required=True,       type=int, help="versionCode (getal)")
    parser.add_argument("--version-name",   required=True,       help="versionName (bijv. 1.4)")
    parser.add_argument("--notes",          default="",          help="Release notes (optioneel)")
    parser.add_argument("--mandatory",      action="store_true", help="Verplichte update")
    parser.add_argument("--github-token",   default=None,        help="GitHub Personal Access Token")
    parser.add_argument("--key-file",       default=None,        help="Pad naar Firestore service account JSON")
    args = parser.parse_args()

    # ── Validatie ─────────────────────────────────────────────────────────────
    github_token = args.github_token or os.environ.get("GITHUB_TOKEN")
    key_file     = args.key_file     or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")

    if not github_token:
        print("❌ Geef --github-token mee of stel GITHUB_TOKEN in")
        print("   Token aanmaken: https://github.com/settings/tokens  (scope: repo)")
        sys.exit(1)

    if not key_file:
        print("❌ Geef --key-file mee of stel GOOGLE_APPLICATION_CREDENTIALS in")
        sys.exit(1)

    if not os.path.exists(args.apk):
        print(f"❌ APK niet gevonden: {args.apk}")
        sys.exit(1)

    # ── GitHub Release aanmaken ───────────────────────────────────────────────
    print(f"🐙 GitHub Release v{args.version_name} aanmaken...")
    release = create_github_release(github_token, f"v{args.version_name}",
                                    args.version_name, args.notes)
    print(f"✅ Release aangemaakt: {release['html_url']}")

    # ── APK uploaden naar GitHub ──────────────────────────────────────────────
    download_url = upload_apk_to_release(github_token, release, args.apk)

    # ── Firestore bijwerken ───────────────────────────────────────────────────
    update_firestore(key_file, args.version_code, args.version_name,
                     download_url, args.notes, args.mandatory, args.apk)

    apk_name = os.path.basename(args.apk)
    print(f"""
╔══════════════════════════════════════════════════════════╗
║  ✅ Release v{args.version_name} succesvol gepubliceerd!
║
║  📦 APK:      {apk_name[:50]}
║  🔢 Code:     {args.version_code}
║  🐙 GitHub:   https://github.com/{GITHUB_REPO}/releases
║  🔔 Gebruikers krijgen nu automatisch een update popup!
╚══════════════════════════════════════════════════════════╝
""")


if __name__ == "__main__":
    main()
