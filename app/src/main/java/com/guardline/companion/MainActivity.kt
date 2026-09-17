package com.guardline.companion

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Setiap izin punya urutan yang SAMA di seluruh app:
 *   1. Tampilkan kartu penjelasan ("izin ini buat apa") -> tombol "Izinkan"
 *   2. Baru setelah anak menekan "Izinkan", sistem Android yang memunculkan
 *      dialog izin resminya (device admin / runtime permission / settings page)
 *   3. Status hasilnya (diberikan / ditolak) disimpan dan ditampilkan lagi
 *      di HomeScreen supaya selalu terlihat, tidak diam-diam.
 *
 * Tidak ada permintaan izin yang dipicu tanpa kartu penjelasan ini dulu.
 */

enum class Screen { WELCOME, PAIR, PERM_LOCK, PERM_USAGE, PERM_LOCATION, PERM_BLOCK, HOME }

class MainActivity : ComponentActivity() {

    private lateinit var lockController: LockController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockController = LockController(this)

        setContent {
            MaterialTheme {
                GuardlineApp(lockController = lockController, activity = this)
            }
        }
    }
}

@Composable
fun GuardlineApp(lockController: LockController, activity: ComponentActivity) {
    var screen by remember { mutableStateOf(Screen.WELCOME) }
    var pairCode by remember { mutableStateOf("") }

    var lockGranted by remember { mutableStateOf(lockController.isAdminActive()) }
    var usageGranted by remember { mutableStateOf(hasUsageAccess(activity)) }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompatCheckLocation(activity)
        )
    }

    // Launcher: minta jadi Device Admin (untuk fitur kunci layar)
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        lockGranted = lockController.isAdminActive()
        screen = Screen.PERM_USAGE
    }

    // Launcher: minta izin lokasi runtime
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result.values.all { it }
        screen = Screen.HOME
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.WELCOME -> WelcomeScreen(onNext = { screen = Screen.PAIR })

            Screen.PAIR -> PairScreen(
                code = pairCode,
                onCodeChange = { pairCode = it },
                onNext = {
                    PairingStore.savePairCode(activity, pairCode)
                    screen = Screen.PERM_LOCK
                }
            )

            Screen.PERM_LOCK -> PermissionExplainScreen(
                title = "Kunci layar dari jarak jauh",
                explanation = "Orang tua bisa mengunci HP ini, misalnya saat waktu belajar atau tidur. Guardline TIDAK bisa menghapus data atau mengubah kata sandi — hanya mengunci layar.",
                onAllow = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, lockController.adminComponentName())
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Guardline Companion memakai izin ini hanya untuk mengunci layar."
                        )
                    }
                    deviceAdminLauncher.launch(intent)
                },
                onSkip = { screen = Screen.PERM_USAGE }
            )

            Screen.PERM_USAGE -> PermissionExplainScreen(
                title = "Lihat aplikasi & waktu pakai",
                explanation = "Orang tua bisa melihat aplikasi apa saja yang dipakai dan berapa lama. Ini butuh halaman khusus di Setelan Android (bukan pop-up biasa) karena aturan Android sendiri.",
                onAllow = {
                    activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    // Anak kembali ke app secara manual setelah mengaktifkan di Setelan;
                    // status dicek ulang saat HomeScreen tampil (lihat hasUsageAccess).
                    screen = Screen.PERM_LOCATION
                },
                onSkip = { screen = Screen.PERM_LOCATION }
            )

            Screen.PERM_LOCATION -> PermissionExplainScreen(
                title = "Bagikan lokasi HP",
                explanation = "Orang tua bisa melihat lokasi HP ini di peta dashboard mereka. Kamu bisa mematikan ini kapan saja dari menu izin Guardline.",
                onAllow = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onSkip = { screen = Screen.PERM_BLOCK }
            )

            Screen.PERM_BLOCK -> PermissionExplainScreen(
                title = "Blokir aplikasi & batas waktu layar",
                explanation = "Orang tua bisa memblokir aplikasi tertentu dan mengatur batas waktu layar harian. Ini butuh Accessibility Service Android — sistem akan menampilkan penjelasannya sendiri sebelum kamu aktifkan di Setelan.",
                onAllow = {
                    activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    screen = Screen.HOME
                },
                onSkip = { screen = Screen.HOME }
            )

            Screen.HOME -> {
                LaunchedEffect(pairCode) {
                    if (pairCode.isNotBlank()) {
                        val svcIntent = Intent(activity, GuardlineForegroundService::class.java)
                            .putExtra(GuardlineForegroundService.EXTRA_PAIR_CODE, pairCode)
                        activity.startForegroundService(svcIntent)
                    }
                }
                HomeScreen(
                    lockGranted = lockController.isAdminActive(),
                    usageGranted = hasUsageAccess(activity),
                    locationGranted = ContextCompatCheckLocation(activity),
                    pairCode = pairCode,
                    onLockNow = { lockController.lockNow() }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aplikasi ini dipasang oleh orang tuamu", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Guardline Companion menghubungkan HP ini ke HP orang tuamu. " +
                "Setiap izin yang diminta akan dijelaskan dulu sebelum kamu setujui.",
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Lanjutkan") }
    }
}

@Composable
fun PairScreen(code: String, onCodeChange: (String) -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Masukkan kode pairing", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Minta orang tuamu membuka dashboard Guardline untuk mendapatkan kode 6 digit.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text("Kode") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Sambungkan") }
    }
}

@Composable
fun PermissionExplainScreen(
    title: String,
    explanation: String,
    onAllow: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(explanation, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) { Text("Izinkan") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text("Lewati untuk sekarang") }
    }
}

@Composable
fun HomeScreen(
    lockGranted: Boolean,
    usageGranted: Boolean,
    locationGranted: Boolean,
    pairCode: String,
    onLockNow: () -> Unit
) {
    var blockedApps by remember { mutableStateOf(listOf<String>()) }
    var limitMinutes by remember { mutableStateOf(0L) }

    DisposableEffect(pairCode) {
        val sync = if (pairCode.isNotBlank()) RealtimeSync(pairCode) else null
        sync?.listenForConfig { blocked, limit ->
            blockedApps = blocked
            limitMinutes = limit
        }
        onDispose { }
    }

    Column(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        Text("HP ini terhubung", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        PermissionStatusRow("Kunci layar", lockGranted)
        PermissionStatusRow("Waktu pakai aplikasi", usageGranted)
        PermissionStatusRow("Lokasi", locationGranted)

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLockNow,
            enabled = lockGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (lockGranted) "Kunci HP sekarang" else "Izin kunci belum diberikan")
        }

        Spacer(Modifier.height(28.dp))
        Text("Aturan dari orang tua", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (limitMinutes > 0) "Batas waktu layar: $limitMinutes menit/hari" else "Belum ada batas waktu layar",
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (blockedApps.isEmpty()) "Belum ada aplikasi yang diblokir"
            else "Aplikasi diblokir: ${blockedApps.joinToString(", ")}",
            fontSize = 14.sp
        )
    }
}

@Composable
fun PermissionStatusRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 15.sp)
        Text(if (granted) "Aktif" else "Belum aktif", fontSize = 14.sp)
    }
}

/** Usage Access adalah "special access" — tidak lewat dialog runtime biasa. */
fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun ContextCompatCheckLocation(context: Context): Boolean {
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}
