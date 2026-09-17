package com.guardline.companion

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

/**
 * Struktur data di Firebase Realtime Database:
 *
 * devices/{pairCode}/
 *   status/          -> ditulis OLEH companion, DIBACA oleh dashboard
 *       paired: Boolean
 *       lockActive: Boolean
 *       permissionLock: Boolean
 *       permissionUsage: Boolean
 *       permissionLocation: Boolean
 *       lastSeen: Long (millis)
 *   commands/
 *       lock: Boolean   -> ditulis OLEH dashboard, DIBACA/didengarkan oleh companion
 *   config/
 *       blockedApps: List<String>       -> daftar packageName yang diblokir
 *       screenTimeLimitMinutes: Long    -> 0 = tidak ada batas
 *   usage/
 *       {packageName}: Long (menit hari ini) -> ditulis OLEH companion
 */
class RealtimeSync(private val pairCode: String) {

    private val root = FirebaseDatabase.getInstance().reference.child("devices").child(pairCode)

    fun writeStatus(
        paired: Boolean,
        lockActive: Boolean,
        permissionLock: Boolean,
        permissionUsage: Boolean,
        permissionLocation: Boolean
    ) {
        val statusMap = mapOf(
            "paired" to paired,
            "lockActive" to lockActive,
            "permissionLock" to permissionLock,
            "permissionUsage" to permissionUsage,
            "permissionLocation" to permissionLocation,
            "lastSeen" to System.currentTimeMillis()
        )
        root.child("status").updateChildren(statusMap)
    }

    /** Dipanggil sekali saat foreground service mulai. Terus mendengarkan perintah kunci. */
    fun listenForLockCommand(onLockRequested: () -> Unit) {
        root.child("commands").child("lock").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shouldLock = snapshot.getValue(Boolean::class.java) ?: false
                if (shouldLock) {
                    onLockRequested()
                    // reset flag supaya tidak terus-terusan trigger
                    root.child("commands").child("lock").setValue(false)
                }
            }
            override fun onCancelled(error: DatabaseError) { /* abaikan untuk prototipe ini */ }
        })
    }

    fun listenForConfig(onConfigChanged: (blockedApps: List<String>, limitMinutes: Long) -> Unit) {
        root.child("config").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val blocked = snapshot.child("blockedApps").children.mapNotNull { it.getValue(String::class.java) }
                val limit = snapshot.child("screenTimeLimitMinutes").getValue(Long::class.java) ?: 0L
                onConfigChanged(blocked, limit)
            }
            override fun onCancelled(error: DatabaseError) { /* abaikan untuk prototipe ini */ }
        })
    }

    fun writeUsageMinutes(packageName: String, minutesToday: Long) {
        root.child("usage").child(packageName.replace(".", "_")).setValue(minutesToday)
    }
}
