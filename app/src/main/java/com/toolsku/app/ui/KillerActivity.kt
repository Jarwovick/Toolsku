package com.toolsku.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toolsku.app.ui.theme.*

class KillerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KillerScreen()
        }
    }

    @Composable
    fun KillerScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hentikan Aplikasi (Killer)",
                color = TextWhite,
                fontSize = 20.sp
            )
        }
    }
}
