package com.toolsku.app.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolsku.app.service.ScreenDimmerService
import com.toolsku.app.service.ToolskuAccessibilityService
import com.toolsku.app.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToolskuDashboard()
        }
    }

    override fun onResume() {
        super.onResume()
        // Memastikan tampilan refresh saat kembali dari halaman Pengaturan Izin
        setContent {
            ToolskuDashboard()
        }
    }

    @Composable
    fun ToolskuDashboard() {
        var brightnessLevel by remember { mutableStateOf(50f) }
        var isDimmerOn by remember { mutableStateOf(ScreenDimmerService.isRunning) }

        val isAccessibilityGranted = isAccessibilityServiceEnabled()
        val isOverlayGranted = Settings.canDrawOverlays(this@MainActivity)
        val isUsageStatsGranted = isUsageStatsPermissionGranted()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(20.dp)
        ) {
            // Header Title
            Text(
                text = "Toolsku",
                color = PrimaryCyan,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Banner Peringatan Izin (jika ada izin yang belum aktif)
            if (!isAccessibilityGranted || !isOverlayGranted || !isUsageStatsGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { openPermissionSettings() },
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚠️ Izin Diperlukan",
                            color = PrimaryCyan,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketuk di sini untuk mengaktifkan Aksesibilitas, Display Overlay, dan Akses Penggunaan agar fitur bekerja.",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 1. Hentikan Aplikasi (Killer)
            MenuCard(
                title = "Hentikan Aplikasi",
                description = "Paksa berhenti aplikasi berjalan",
                onClick = {
                    val intent = Intent(this@MainActivity, KillerActivity::class.java)
                    startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Bersihkan Cache (Cleaner)
            MenuCard(
                title = "Bersihkan Cache",
                description = "Hapus cache semua aplikasi",
                onClick = {
                    val intent = Intent(this@MainActivity, CacheCleanerActivity::class.java)
                    startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Peredupan Layar (Lower Brightness)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Peredupan Layar", color = TextWhite, fontSize = 18.sp)
                            Text(
                                text = "Atur tingkat kecerahan layar",
                                color = TextGray,
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = isDimmerOn,
                            onCheckedChange = { active ->
                                if (active) {
                                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:$packageName")
                                        )
                                        startActivity(intent)
                                    } else {
                                        isDimmerOn = true
                                        ScreenDimmerService.updateBrightness(
                                            this@MainActivity,
                                            brightnessLevel.toInt()
                                        )
                                    }
                                } else {
                                    isDimmerOn = false
                                    stopService(
                                        Intent(
                                            this@MainActivity,
                                            ScreenDimmerService::class.java
                                        )
                                    )
                                }
                            }
                        )
                    }

                    if (isDimmerOn) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tingkat: ${brightnessLevel.toInt()}%",
                            color = PrimaryCyan,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = brightnessLevel,
                            onValueChange = { value ->
                                brightnessLevel = value
                                ScreenDimmerService.updateBrightness(
                                    this@MainActivity,
                                    value.toInt()
                                )
                            },
                            valueRange = 0f..100f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Menu Boot
            MenuCard(
                title = "Menu Boot",
                description = "Restart atau matikan perangkat",
                onClick = {
                    val service = ToolskuAccessibilityService.instance
                    if (service != null) {
                        service.openPowerMenu()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Aktifkan Layanan Aksesibilitas terlebih dahulu",
                            Toast.LENGTH_SHORT
                        ).show()
                        openPermissionSettings()
                    }
                }
            )
        }
    }

    @Composable
    fun MenuCard(title: String, description: String, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = title, color = TextWhite, fontSize = 18.sp)
                Text(text = description, color = TextGray, fontSize = 13.sp)
            }
        }
    }

    // --- FUNGSI MANAJEMEN IZIN & VALIDASI ---

    private fun openPermissionSettings() {
        if (!isAccessibilityServiceEnabled()) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else if (!isUsageStatsPermissionGranted()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${ToolskuAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedService)
    }

    private fun isUsageStatsPermissionGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
