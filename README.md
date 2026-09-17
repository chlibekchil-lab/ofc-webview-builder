# Guardline Companion — project Android

Ini kerangka project Android Studio untuk apk companion Guardline (sisi HP anak),
hasil konversi dari prototipe web sebelumnya.

## Cara membuka
1. Buka Android Studio → **Open** → pilih folder `GuardlineCompanion` ini.
2. Tunggu Gradle sync selesai (perlu koneksi internet untuk download dependency
   pertama kali).
3. Jalankan ke emulator atau HP fisik (Run ▶).

## Prinsip alur izin di seluruh app
Untuk setiap fitur, urutannya **selalu**:
1. Kartu penjelasan dulu ("izin ini buat apa") — lihat `PermissionExplainScreen`
   di `MainActivity.kt`.
2. Baru setelah anak menekan "Izinkan", dialog izin resmi Android muncul.
3. Status (aktif/belum) selalu terlihat lagi di `HomeScreen` — tidak ada yang
   berjalan diam-diam di belakang tanpa status yang terlihat.

## Fitur yang sudah diimplementasikan
- **Kunci layar** — lewat Device Administrator API (`LockController.lockNow()`).
  Policy yang diminta ke sistem cuma `force-lock`, sengaja tidak minta hak
  wipe-data atau reset-password.
- **Sinkron ke dashboard lewat Firebase Realtime Database** (`RealtimeSync.kt`).
  Companion menulis status (izin apa yang aktif, status terkunci) dan
  mendengarkan perintah kunci dari dashboard. **WAJIB isi `FirebaseConfig.kt`
  dengan data project Firebase kamu sendiri** — lihat komentar di file itu.
- **Foreground service** (`GuardlineForegroundService.kt`) — menjaga koneksi
  Firebase tetap hidup walau app ditutup, dengan notifikasi permanen yang
  SELALU terlihat oleh anak (sengaja tidak disembunyikan).
- **Blokir aplikasi & batas waktu layar** lewat Accessibility Service
  (`GuardlineAccessibilityService.kt`) — mengaktifkannya lewat dialog
  penjelasan resmi Android sendiri, membaca daftar blokir & batas waktu dari
  Firebase (diatur dari dashboard), TIDAK membaca isi layar
  (`canRetrieveWindowContent="false"`).

## Yang masih perlu kamu kerjakan
1. **Isi `FirebaseConfig.kt`** dengan project Firebase kamu sendiri (gratis,
   lihat instruksi di file itu).
2. **Sambungkan dashboard web (`guardline-dashboard.html`) ke Firebase project
   yang sama** agar tombol "Kunci HP sekarang" dan menu blokir aplikasi di
   dashboard benar-benar menulis ke `devices/{pairCode}/commands` dan
   `devices/{pairCode}/config`. Catatan: karena dashboard saat ini dipublish
   sebagai Claude Artifact, Content-Security-Policy artifact TIDAK
   mengizinkan memuat Firebase JS SDK (domain gstatic.com tidak ada di daftar
   izin). Supaya Firebase benar-benar jalan di dashboard, dashboard perlu
   dideploy sebagai website sungguhan (misalnya lewat Netlify/Vercel yang
   sudah terhubung) — bisa aku bantu proses deploy-nya kalau mau lanjut ke
   sini.
3. **Lokasi real-time** — belum ditulis; perlu `FusedLocationProviderClient`
   + kirim ke `RealtimeSync` (pola penulisannya sama seperti `writeStatus`).
4. Uji di HP fisik: emulator biasanya tidak mendukung Accessibility Service
   dan Device Admin dengan baik.

## Tentang icon & tema
Icon dan tema di sini masih placeholder sederhana (warna hijau + huruf G) —
ganti sesuai kebutuhan lewat Android Studio > Image Asset Studio.
