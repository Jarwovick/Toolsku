package com.toolsku.app.cleaner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

class CleanerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        title = getString(R.string.cleaner_title)
    }
}
