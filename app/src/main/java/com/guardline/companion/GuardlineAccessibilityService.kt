package com.guardline.companion

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

/**
 * PENTING soal transparansi: mengaktifkan Accessibility Service selalu
 * menampilkan dialog sistem Android sendiri yang menjelaskan apa yang bisa
 * dilakukan service ini (melihat & mengontrol layar). Kita TIDAK melewati
 * dialog itu — anak/orang tua yang mengaktifkan dari Setelan Android
 * setelah membaca kartu penjelasan companion (lihat PermissionExplainScreen
 * untuk "Blokir aplikasi" di MainActivity.kt — perlu ditambahkan).
 *
 * Fungsinya dua:
 * 1. Kalau paket aplikasi yang sedang dibuka ada di blockedApps -> tekan
 *    tombol Home (GLOBAL_ACTION_HOME) supaya aplikasi itu tertutup.
 * 2. Kalau total screen time hari ini sudah lewat batas -> semua aplikasi
 *    ditutup ke Home juga (kecuali launcher).
 */
class GuardlineAccessibilityService : AccessibilityService() {

    private var blockedApps: List<String> = emptyList()
    private var screenTimeLimitMinutes: Long = 0L
    private var pairCode: String? = null
    private var sync: RealtimeSync? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        pairCode = PairingStore.getPairCode(this)
        pairCode?.let { code ->
            sync = RealtimeSync(code).also {
                it.listenForConfig { blocked, limit ->
                    blockedApps = blocked
                    screenTimeLimitMinutes = limit
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == packageName) return // jangan blokir Guardline sendiri

        if (blockedApps.contains(pkg)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        if (screenTimeLimitMinutes > 0 && todayScreenTimeMinutes() >= screenTimeLimitMinutes) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun todayScreenTimeMinutes(): Long {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
        val totalMs = stats?.sumOf { it.totalTimeInForeground } ?: 0L
        return totalMs / 60000
    }

    override fun onInterrupt() { /* tidak perlu aksi khusus */ }
}
