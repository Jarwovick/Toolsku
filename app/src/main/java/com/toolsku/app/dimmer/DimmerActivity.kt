package com.toolsku.app.dimmer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

class DimmerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        title = getString(R.string.dimmer_title)
    }
}
