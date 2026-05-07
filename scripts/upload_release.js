#!/usr/bin/env node
/**
 * upload_release.js — Upload APK naar GitHub Releases en update versie in Firestore.
 *
 * Gebruik:
 *   node scripts/upload_release.js \
 *     --apk app/build/outputs/apk/release/BoodschappenlijstApp-v1.4.apk \
 *     --version-code 5 \
 *     --version-name 1.4 \
 *     --notes "Moderne UI, dark/light theme, in-app updates" \
 *     --github-token ghp_XXXX \
 *     --key-file pad/naar/service-account.json
 *
 * Installeer dependencies eenmalig:
 *   npm install firebase-admin node-fetch@2
 */

const fs    = require("fs");
const path  = require("path");
const https = require("https");

// ── Argumenten parsen ─────────────────────────────────────────────────────────
const args = {};
process.argv.slice(2).forEach((arg, i, arr) => {
  if (arg.startsWith("--")) {
    const key = arg.slice(2);
    const val = arr[i + 1] && !arr[i + 1].startsWith("--") ? arr[i + 1] : true;
    args[key] = val;
  }
});

const APK_PATH      = args["apk"];
const VERSION_CODE  = parseInt(args["version-code"], 10);
const VERSION_NAME  = args["version-name"];
const NOTES         = args["notes"] || "";
const MANDATORY     = args["mandatory"] === true;
const GITHUB_TOKEN  = args["github-token"] || process.env.GITHUB_TOKEN;
const KEY_FILE      = args["key-file"]     || process.env.GOOGLE_APPLICATION_CREDENTIALS;
const GITHUB_REPO   = "sabair24/BoodschappenApp";

// ── Validatie ─────────────────────────────────────────────────────────────────
if (!APK_PATH || !VERSION_CODE || !VERSION_NAME) {
  console.error("❌ Gebruik: node scripts/upload_release.js --apk <pad> --version-code <n> --version-name <v>");
  process.exit(1);
}
if (!GITHUB_TOKEN) {
  console.error("❌ Geef --github-token mee of stel GITHUB_TOKEN in");
  console.error("   Token aanmaken: https://github.com/settings/tokens  (scope: repo)");
  process.exit(1);
}
if (!KEY_FILE) {
  console.error("❌ Geef --key-file mee of stel GOOGLE_APPLICATION_CREDENTIALS in");
  process.exit(1);
}
if (!fs.existsSync(APK_PATH)) {
  console.error(`❌ APK niet gevonden: ${APK_PATH}`);
  process.exit(1);
}

// ── GitHub API helper ─────────────────────────────────────────────────────────
function githubRequest(method, urlPath, body, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: "api.github.com",
      path:     urlPath,
      method,
      headers: {
        "Authorization": `token ${GITHUB_TOKEN}`,
        "Accept":        "application/vnd.github.v3+json",
        "User-Agent":    "BoodschappenApp-release-script",
        ...extraHeaders,
      },
    };
    if (body) {
      const data = typeof body === "string" ? body : JSON.stringify(body);
      options.headers["Content-Type"]   = "application/json";
      options.headers["Content-Length"] = Buffer.byteLength(data);
    }

    const req = https.request(options, (res) => {
      let raw = "";
      res.on("data", chunk => raw += chunk);
      res.on("end", () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(raw) }); }
        catch { resolve({ status: res.statusCode, body: raw }); }
      });
    });
    req.on("error", reject);
    if (body) req.write(typeof body === "string" ? body : JSON.stringify(body));
    req.end();
  });
}

// ── Upload bestand via upload.github.com ──────────────────────────────────────
function uploadAsset(uploadUrl, filePath) {
  return new Promise((resolve, reject) => {
    const fileName    = path.basename(filePath);
    const fileBuffer  = fs.readFileSync(filePath);
    const fileSize    = fileBuffer.length;

    // upload_url heeft de vorm: https://uploads.github.com/repos/.../releases/.../assets{?name,label}
    const base = uploadUrl.replace("{?name,label}", "");
    const url  = new URL(base);

    const options = {
      hostname: url.hostname,
      path:     `${url.pathname}?name=${encodeURIComponent(fileName)}`,
      method:   "POST",
      headers: {
        "Authorization": `token ${GITHUB_TOKEN}`,
        "Accept":        "application/vnd.github.v3+json",
        "Content-Type":  "application/vnd.android.package-archive",
        "Content-Length": fileSize,
        "User-Agent":    "BoodschappenApp-release-script",
      },
    };

    const req = https.request(options, (res) => {
      let raw = "";
      res.on("data", chunk => raw += chunk);
      res.on("end", () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(raw) }); }
        catch { resolve({ status: res.statusCode, body: raw }); }
      });
    });
    req.on("error", reject);

    // Voortgang tonen
    let sent = 0;
    let lastPct = -1;
    const stream = fs.createReadStream(filePath);
    stream.on("data", chunk => {
      sent += chunk.length;
      const pct = Math.floor((sent / fileSize) * 100);
      if (pct !== lastPct && pct % 10 === 0) {
        process.stdout.write(`\r   ⬆️  ${pct}%`);
        lastPct = pct;
      }
      req.write(chunk);
    });
    stream.on("end", () => { console.log(""); req.end(); });
    stream.on("error", reject);
  });
}

// ── Hoofd script ──────────────────────────────────────────────────────────────
async function main() {
  const apkName   = path.basename(APK_PATH);
  const apkSizeMb = (fs.statSync(APK_PATH).size / (1024 * 1024)).toFixed(1);

  console.log(`\n🚀 Boodschappenlijst App — Release v${VERSION_NAME} uploaden`);
  console.log(`   APK: ${apkName} (${apkSizeMb} MB)\n`);

  // ── Stap 1: GitHub Release aanmaken ────────────────────────────────────────
  console.log("🐙 GitHub Release aanmaken...");
  let release;
  const createRes = await githubRequest("POST", `/repos/${GITHUB_REPO}/releases`, {
    tag_name:         `v${VERSION_NAME}`,
    target_commitish: "main",
    name:             `v${VERSION_NAME}`,
    body:             NOTES || `Release v${VERSION_NAME}`,
    draft:            false,
    prerelease:       false,
  });

  if (createRes.status === 422) {
    console.log("⚠️  Release bestaat al, gebruik bestaande release...");
    const getRes = await githubRequest("GET", `/repos/${GITHUB_REPO}/releases/tags/v${VERSION_NAME}`);
    if (getRes.status !== 200) {
      console.error("❌ Kon bestaande release niet ophalen:", getRes.body);
      process.exit(1);
    }
    release = getRes.body;
  } else if (createRes.status === 201) {
    release = createRes.body;
    console.log(`✅ Release aangemaakt: ${release.html_url}`);
  } else {
    console.error("❌ GitHub Release aanmaken mislukt:", createRes.status, createRes.body);
    process.exit(1);
  }

  // Verwijder bestaand asset met dezelfde naam
  if (release.assets) {
    for (const asset of release.assets) {
      if (asset.name === apkName) {
        console.log(`   ♻️  Verwijder oud asset: ${apkName}`);
        await githubRequest("DELETE", `/repos/${GITHUB_REPO}/releases/assets/${asset.id}`);
      }
    }
  }

  // ── Stap 2: APK uploaden ────────────────────────────────────────────────────
  console.log(`📤 APK uploaden naar GitHub...`);
  const uploadRes = await uploadAsset(release.upload_url, APK_PATH);
  if (uploadRes.status !== 201) {
    console.error("❌ Upload mislukt:", uploadRes.status, uploadRes.body);
    process.exit(1);
  }
  const downloadUrl = uploadRes.body.browser_download_url;
  console.log(`✅ Upload klaar!`);
  console.log(`🔗 Download URL: ${downloadUrl}\n`);

  // ── Stap 3: Firestore bijwerken ─────────────────────────────────────────────
  console.log("🔥 Firestore bijwerken...");
  const admin = require("firebase-admin");
  const serviceAccount = JSON.parse(fs.readFileSync(KEY_FILE, "utf8"));

  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      projectId:  "boodschappenlijst-app-claude",
    });
  }

  const db  = admin.firestore();
  const doc = {
    versionCode:  VERSION_CODE,
    versionName:  VERSION_NAME,
    downloadUrl,
    releaseNotes: NOTES,
    mandatory:    MANDATORY,
    uploadedAt:   admin.firestore.FieldValue.serverTimestamp(),
    fileName:     apkName,
    sizeMb:       parseFloat(apkSizeMb),
  };

  await db.collection("app_updates").doc("latest").set(doc);
  await db.collection("app_updates").doc(`v${VERSION_NAME}`).set(doc);
  console.log("✅ Firestore bijgewerkt!\n");

  console.log(`╔══════════════════════════════════════════════════════════╗`);
  console.log(`║  ✅ Release v${VERSION_NAME} succesvol gepubliceerd!`);
  console.log(`║`);
  console.log(`║  📦 APK:    ${apkName}`);
  console.log(`║  🔢 Code:   ${VERSION_CODE}`);
  console.log(`║  🐙 GitHub: https://github.com/${GITHUB_REPO}/releases`);
  console.log(`║  🔔 Gebruikers krijgen nu automatisch een update popup!`);
  console.log(`╚══════════════════════════════════════════════════════════╝\n`);

  process.exit(0);
}

main().catch(err => {
  console.error("❌ Fout:", err.message || err);
  process.exit(1);
});
