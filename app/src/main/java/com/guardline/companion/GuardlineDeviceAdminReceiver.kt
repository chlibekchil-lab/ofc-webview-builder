package com.guardline.companion

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Wajib ada agar Android mengizinkan apk ini menjadi "Device Administrator".
 * Hanya dipakai untuk satu hal: mengunci layar (lihat DevicePolicyManager.lockNow()
 * di LockController.kt). Tidak ada implementasi wipe-data atau ubah password di sini,
 * sesuai policy yang dideklarasikan di res/xml/device_admin.xml (hanya <force-lock/>).
 */
class GuardlineDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
