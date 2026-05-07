#!/usr/bin/env node
/**
 * update_firestore_ci.js
 * Wordt aangeroepen door GitHub Actions na een release.
 * Leest alles uit environment variables — geen bestanden nodig.
 */

const admin = require("firebase-admin");

const VERSION_NAME   = process.env.VERSION_NAME;
const APK_NAME       = process.env.APK_NAME;
const REPO           = process.env.GITHUB_REPOSITORY;  // sabair24/BoodschappenApp
const SA_JSON        = process.env.FIREBASE_SERVICE_ACCOUNT;

if (!VERSION_NAME || !APK_NAME || !REPO || !SA_JSON) {
  console.error("❌ Ontbrekende environment variables");
  process.exit(1);
}

// Versienummer omzetten naar versionCode (bijv. "1.5" → 6, "1.4" → 5)
// Formule: major * 100 + minor  (1.5 → 105, 1.4 → 104, enz.)
const [major, minor] = VERSION_NAME.split(".").map(Number);
const versionCode = major * 100 + minor;

const downloadUrl = `https://github.com/${REPO}/releases/download/v${VERSION_NAME}/${APK_NAME}`;

const serviceAccount = JSON.parse(SA_JSON);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId:  serviceAccount.project_id,
});

const db  = admin.firestore();
const doc = {
  versionCode,
  versionName:  VERSION_NAME,
  downloadUrl,
  releaseNotes: "",
  mandatory:    false,
  uploadedAt:   admin.firestore.FieldValue.serverTimestamp(),
  fileName:     APK_NAME,
};

(async () => {
  await db.collection("app_updates").doc("latest").set(doc);
  await db.collection("app_updates").doc(`v${VERSION_NAME}`).set(doc);
  console.log(`✅ Firestore bijgewerkt: v${VERSION_NAME} (code ${versionCode})`);
  console.log(`🔗 ${downloadUrl}`);
  process.exit(0);
})().catch(err => {
  console.error("❌", err.message);
  process.exit(1);
});
