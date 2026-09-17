package com.guardline.companion

/**
 * ISI INI DENGAN DATA DARI FIREBASE CONSOLE PROJECT KAMU SENDIRI.
 *
 * Cara dapatnya:
 * 1. Buka https://console.firebase.google.com -> buat project baru (gratis).
 * 2. Tambah app Android dengan applicationId "com.guardline.companion".
 * 3. Aktifkan "Realtime Database" (mode test dulu untuk development).
 * 4. Buka Project Settings -> General -> scroll ke "Your apps" -> lihat
 *    konfigurasi SDK (apiKey, appId, dst) dan salin ke bawah ini.
 * 5. Salin juga URL Realtime Database-nya (dari tab Realtime Database).
 *
 * Dashboard web (guardline-dashboard.html) juga perlu disambungkan ke
 * Firebase project YANG SAMA supaya companion & dashboard bisa saling kirim
 * data. Beri tahu saya kalau butuh bantuan menyambungkan dashboard webnya.
 */
object FirebaseConfig {
    const val API_KEY = "AIzaSyAgoT_EfuSV-kNbo0NB0McnwEOt7yj6oRg"
    const val APPLICATION_ID = "1:897376387302:web:6fd71c02e2867cb0007b27"
    const val PROJECT_ID = "guardline-2054a"
    const val DATABASE_URL = "https://guardline-2054a-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val STORAGE_BUCKET = "guardline-2054a.firebasestorage.app"
}
