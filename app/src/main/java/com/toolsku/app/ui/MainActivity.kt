package com.toolsku.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
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

    @Composable
    fun ToolskuDashboard() {
        var brightnessLevel by remember { mutableStateOf(50f) }
        var isDimmerOn by remember { mutableStateOf(ScreenDimmerService.isRunning) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(20.dp)
        ) {
            // Title Bar
            Text(
                text = "Toolsku",
                color = PrimaryCyan,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Hentikan Aplikasi (Killer)
            MenuCard(
                title = "Hentikan Aplikasi",
                description = "Paksa berhenti aplikasi berjalan",
                onClick = {
                    val intent = Intent(this@MainActivity, KillerActivity::class.java)
                    startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bersihkan Cache
            MenuCard(
                title = "Bersihkan Cache",
                description = "Hapus cache semua aplikasi",
                onClick = {
                    val intent = Intent(this@MainActivity, CacheCleanerActivity::class.java)
                    startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                            Text(text = "Atur tingkat kecerahan layar", color = TextGray, fontSize = 13.sp)
                        }
                        Switch(
                            checked = isDimmerOn,
                            onCheckedChange = { active ->
                                if (active) {
                                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                        startActivity(intent)
                                    } else {
                                        isDimmerOn = true
                                        ScreenDimmerService.updateBrightness(this@MainActivity, brightnessLevel.toInt())
                                    }
                                } else {
                                    isDimmerOn = false
                                    stopService(Intent(this@MainActivity, ScreenDimmerService::class.java))
                                }
                            }
                        )
                    }

                    if (isDimmerOn) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Tingkat: ${brightnessLevel.toInt()}%", color = PrimaryCyan, fontSize = 14.sp)
                        Slider(
                            value = brightnessLevel,
                            onValueChange = { value ->
                                brightnessLevel = value
                                ScreenDimmerService.updateBrightness(this@MainActivity, value.toInt())
                            },
                            valueRange = 0f..100f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Menu Boot
            MenuCard(
                title = "Menu Boot",
                description = "Restart atau matikan perangkat",
                onClick = {
                    val service = ToolskuAccessibilityService.instance
                    if (service != null) {
                        service.openPowerMenu()
                    } else {
                        Toast.makeText(this@MainActivity, "Aktifkan Layanan Aksesibilitas terlebih dahulu", Toast.LENGTH_SHORT).show()
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
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
}
