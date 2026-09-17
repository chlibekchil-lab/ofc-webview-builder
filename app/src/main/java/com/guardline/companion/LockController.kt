package com.guardline.companion

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

/**
 * Satu fungsi sederhana: kunci layar. Dipanggil dari HomeScreen (tombol
 * "Kunci HP sekarang") atau nanti dari layanan latar belakang saat dashboard
 * orang tua mengirim perintah kunci.
 */
class LockController(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, GuardlineDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    fun adminComponentName(): ComponentName = adminComponent

    /** Mengunci layar sekarang. Hanya berhasil jika isAdminActive() == true. */
    fun lockNow(): Boolean {
        if (!isAdminActive()) return false
        dpm.lockNow()
        return true
    }
}
