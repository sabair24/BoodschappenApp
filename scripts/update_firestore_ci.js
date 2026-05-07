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

const VERSION_CODE_RAW = process.env.VERSION_CODE;

if (!VERSION_NAME || !APK_NAME || !REPO || !SA_JSON || !VERSION_CODE_RAW) {
  console.error("❌ Ontbrekende environment variables");
  process.exit(1);
}

// versionCode komt direct uit build.gradle.kts via de workflow
const versionCode = parseInt(process.env.VERSION_CODE, 10);
if (!versionCode || isNaN(versionCode)) {
  console.error("❌ Ontbrekende of ongeldige VERSION_CODE environment variable");
  process.exit(1);
}

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
