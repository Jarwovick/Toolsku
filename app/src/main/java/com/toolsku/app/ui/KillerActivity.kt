package com.toolsku.app.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.toolsku.app.service.ToolskuAccessibilityService
import com.toolsku.app.ui.theme.*

data class AppInfoItem(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    val isSystemApp: Boolean,
    var isSelected: Boolean = true
)

class KillerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KillerScreen()
        }
    }

    @Composable
    fun KillerScreen() {
        val context = this
        var userApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
        var systemApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val uApps = mutableListOf<AppInfoItem>()
            val sApps = mutableListOf<AppInfoItem>()

            for (app in packages) {
                if (app.packageName == context.packageName) continue // Lewati aplikasi Toolsku sendiri
                
                val name = pm.getApplicationLabel(app).toString()
                val icon = pm.getApplicationIcon(app)
                val isSys = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val item = AppInfoItem(name, app.packageName, icon, isSys)
                if (isSys) sApps.add(item) else uApps.add(item)
            }

            userApps = uApps
            systemApps = sApps
            isLoading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hentikan Aplikasi",
                    color = PrimaryCyan,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = {
                    val intent = Intent(context, ExceptionListActivity::class.java)
                    startActivity(intent)
                }) {
                    Text(text = "Exception List", color = PrimaryCyan)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 70.dp)
                    ) {
                        // User Apps Section
                        if (userApps.isNotEmpty()) {
                            item {
                                Text(
                                    text = "USER APPS (${userApps.size})",
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(userApps) { app ->
                                AppRow(app) { checked ->
                                    app.isSelected = checked
                                }
                            }
                        }

                        // System Apps Section
                        if (systemApps.isNotEmpty()) {
                            item {
                                Text(
                                    text = "SYSTEM APPS (${systemApps.size})",
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(systemApps) { app ->
                                AppRow(app) { checked ->
                                    app.isSelected = checked
                                }
                            }
                        }
                    }

                    // Floating Action Button Eksekusi
                    val totalSelected = userApps.count { it.isSelected } + systemApps.count { it.isSelected }
                    Button(
                        onClick = {
                            if (ToolskuAccessibilityService.instance == null) {
                                Toast.makeText(context, "Aktifkan Aksesibilitas terlebih dahulu", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                return@Button
                            }
                            
                            ToolskuAccessibilityService.isAutoStopRunning = true
                            val selectedList = (userApps + systemApps).filter { it.isSelected }
                            if (selectedList.isNotEmpty()) {
                                val firstApp = selectedList.first()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${firstApp.packageName}")
                                }
                                startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                    ) {
                        Text(
                            text = "HENTIKAN APLIKASI ($totalSelected)",
                            color = DarkBackground,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun AppRow(app: AppInfoItem, onCheckedChange: (Boolean) -> Unit) {
        var isChecked by remember { mutableStateOf(app.isSelected) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                        contentDescription = app.name,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = app.name, color = TextWhite, fontSize = 16.sp)
                }
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        isChecked = it
                        onCheckedChange(it)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan)
                )
            }
        }
    }
}
