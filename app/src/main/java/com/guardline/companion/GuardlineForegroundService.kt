package com.guardline.companion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Service ini yang membuat kunci dari dashboard bisa "sampai" ke HP anak
 * walau app Guardline Companion tidak sedang dibuka. Notifikasi permanen
 * SENGAJA selalu ditampilkan (bukan disembunyikan) supaya anak tahu
 * companion sedang aktif memantau — sesuai prinsip transparan yang sudah
 * disepakati.
 */
class GuardlineForegroundService : Service() {

    private lateinit var sync: RealtimeSync
    private lateinit var lockController: LockController

    override fun onCreate() {
        super.onCreate()
        lockController = LockController(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pairCode = intent?.getStringExtra(EXTRA_PAIR_CODE) ?: return START_NOT_STICKY
        sync = RealtimeSync(pairCode)

        startForeground(NOTIF_ID, buildNotification())

        sync.listenForLockCommand {
            lockController.lockNow()
        }

        sync.writeStatus(
            paired = true,
            lockActive = false,
            permissionLock = lockController.isAdminActive(),
            permissionUsage = hasUsageAccess(this),
            permissionLocation = ContextCompatCheckLocation(this)
        )

        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "guardline_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Status Guardline",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Guardline Companion aktif")
            .setContentText("Terhubung ke dashboard orang tua")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_PAIR_CODE = "pair_code"
        const val NOTIF_ID = 1001
    }
}
