# Template Android WebView (buat fitur "Buat APK" di Bot Tim OFC)

Ini template Android minimal yang cuma nampilin sebuah website di dalam
WebView — dipakai bot buat "ubah website jadi APK" secara otomatis lewat
GitHub Actions.

⚠️ **Catatan jujur**: Template ini aku susun berdasarkan pola umum yang
biasa dipakai (bukan hasil compile beneran, karena aku nggak punya
environment Android buat nge-test). Kemungkinan besar percobaan pertama
perlu sedikit debug (misal versi Gradle/Android SDK yang nggak cocok).
Kalau ada error pas build pertama, screenshot log-nya dari tab **Actions**
di GitHub, kirim ke chat ini biar aku bantu perbaiki.

## Cara Setup (Sekali Saja)

1. **Bikin repo baru di GitHub** (boleh private atau public), misal
   namanya `ofc-webview-builder`
2. Upload/push semua isi folder ini ke repo itu (termasuk folder
   `.github/workflows/`)
3. **Bikin Personal Access Token**:
   - Buka https://github.com/settings/tokens
   - "Generate new token" → **classic**
   - Centang scope: `repo` dan `workflow`
   - Generate, copy tokennya (cuma muncul sekali!)
4. Isi di `config.js` bot Node.js kamu:
   ```js
   GITHUB_TOKEN: "isi-token-kamu",
   GITHUB_OWNER: "username-github-kamu",
   GITHUB_REPO: "ofc-webview-builder",
   GITHUB_WORKFLOW_FILE: "build-apk.yml",
   GITHUB_BRANCH: "main",
   ```

## Cara Kerja

1. User klik "📱 Buat APK" di bot → isi nama app & URL website
2. Bot manggil GitHub API buat trigger workflow ini
3. GitHub Actions otomatis: install Android SDK, isi nama app & URL ke
   `strings.xml`, compile APK
4. Bot polling tiap 15 detik nunggu build selesai (bisa 3-8 menit)
5. Kalau sukses, bot download APK-nya dan kirim langsung ke chat

## Kalau Mau Coba Manual Dulu (Tanpa Bot)

Buka tab **Actions** di repo GitHub kamu → pilih workflow "Build APK" →
"Run workflow" → isi nama app & URL → Run. Tunggu selesai, APK-nya bisa
didownload dari halaman run tersebut (bagian "Artifacts").

## Batasan

- APK yang dihasilkan itu **APK debug (belum ditandatangani buat rilis
  ke Play Store)** — cukup buat dipakai sendiri/dibagikan manual, tapi
  belum bisa diupload ke Google Play (butuh proses signing terpisah)
- Ini WebView wrapper doang — bukan aplikasi native asli, jadi fiturnya
  ya sebatas nampilin website (nggak ada notifikasi push, dll kecuali
  ditambah manual)
