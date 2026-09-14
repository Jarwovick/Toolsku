package com.toolsku.app.boot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

class BootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        title = getString(R.string.boot_title)
    }
}
