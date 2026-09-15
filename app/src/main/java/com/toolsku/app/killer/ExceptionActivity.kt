package com.toolsku.app.killer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

class ExceptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Placeholder — akan diisi di Fase 4C
        setContentView(TextView(this).apply {
            text = "Exception List — Coming Soon"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#0B1220"))
            gravity = android.view.Gravity.CENTER
        })
        title = getString(R.string.header_exception_list)
    }
}
