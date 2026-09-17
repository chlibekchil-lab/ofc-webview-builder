package com.guardline.companion

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class GuardlineApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val options = FirebaseOptions.Builder()
            .setApiKey(FirebaseConfig.API_KEY)
            .setApplicationId(FirebaseConfig.APPLICATION_ID)
            .setProjectId(FirebaseConfig.PROJECT_ID)
            .setDatabaseUrl(FirebaseConfig.DATABASE_URL)
            .setStorageBucket(FirebaseConfig.STORAGE_BUCKET)
            .build()

        // Inisialisasi manual (bukan lewat plugin google-services) supaya
        // tidak butuh file google-services.json — cukup isi FirebaseConfig.kt.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options)
        }
    }
}
