package com.toolsku.app.killer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.toolsku.app.R

/**
 * Halaman Hentikan Aplikasi.
 *
 * Fase 4A: Layout saja — belum ada data & adapter.
 * Fase 4B: Akan ditambah RecyclerView + adapter + data.
 */
class KillerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_killer)
        title = getString(R.string.killer_title)
    }
}
