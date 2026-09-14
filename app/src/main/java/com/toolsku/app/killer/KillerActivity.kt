package com.toolsku.app.killer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

class KillerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        title = getString(R.string.killer_title)
    }
}
