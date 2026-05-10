#!/usr/bin/env node
/**
 * update_firestore_local.js
 *
 * Gebruik:  node scripts/update_firestore_local.js [versie] [downloadUrl]
 *
 * Voorbeeld:
 *   node scripts/update_firestore_local.js 1.5 https://github.com/sabair24/BoodschappenApp/releases/download/v1.5/BoodschappenlijstApp-v1.5.apk
 *
 * Vereisten:
 *   1. npm install firebase-admin  (eenmalig)
 *   2. Download je Firebase service-account JSON:
 *      Firebase Console → Project Settings → Service accounts → Generate new private key
 *      Sla op als: scripts/service-account.json  (staat in .gitignore)
 */

const admin = require("firebase-admin");
const fs    = require("fs");
const path  = require("path");

// ── Argumenten ───────────────────────────────────────────────────────────────────────────────
const versionName = process.argv[2];
const downloadUrl = process.argv[3] || "";

if (!versionName) {
  console.error("Gebruik: node scripts/update_firestore_local.js <versie> [downloadUrl]");
  console.error("Voorbeeld: node scripts/update_firestore_local.js 1.5 https://...");
  process.exit(1);
}

// Lees versionCode automatisch uit build.gradle.kts
const gradlePath = path.join(__dirname, "../app/build.gradle.kts");
const gradle     = fs.readFileSync(gradlePath, "utf8");
const match      = gradle.match(/versionCode\s*=\s*(\d+)/);
const versionCode = match ? parseInt(match[1], 10) : null;
if (!versionCode) {
  console.error("❌ Kan versionCode niet lezen uit app/build.gradle.kts");
  process.exit(1);
}

// ── Firebase initialiseren ────────────────────────────────────────────────────────────────
const saPath = path.join(__dirname, "service-account.json");
if (!fs.existsSync(saPath)) {
  console.error(`❌ Geen service-account.json gevonden op: ${saPath}`);
  console.error("   Download hem via: Firebase Console → Project Settings → Service accounts");
  process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(saPath, "utf8"));
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId:  serviceAccount.project_id,
});

// ── Firestore bijwerken ───────────────────────────────────────────────────────────────────
const db  = admin.firestore();
const doc = {
  versionCode,
  versionName,
  downloadUrl,
  releaseNotes: `v${versionName} beschikbaar`,
  mandatory:    false,
  uploadedAt:   admin.firestore.FieldValue.serverTimestamp(),
};

(async () => {
  await db.collection("app_updates").doc("latest").set(doc);
  await db.collection("app_updates").doc(`v${versionName}`).set(doc);
  console.log(`✅ Firestore bijgewerkt: v${versionName} (versionCode ${versionCode})`);
  if (downloadUrl) console.log(`🔗 ${downloadUrl}`);
  else             console.log("⚠️  Geen downloadUrl opgegeven — popup verschijnt maar download werkt niet");
  process.exit(0);
})().catch(err => {
  console.error("❌", err.message);
  process.exit(1);
});
