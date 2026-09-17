package com.toolsku.app.killer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Halaman Killer — placeholder.
 * Akan diisi di fase berikutnya.
 */
class KillerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Placeholder UI
        setContentView(TextView(this).apply {
            text = "Killer — Coming Soon"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#0B1220"))
            gravity = android.view.Gravity.CENTER
        })
    }
}
