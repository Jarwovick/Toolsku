package com.toolsku.app.shortcut

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * SEMENTARA: Shortcut dinonaktifkan selama Killer refactor.
 */
class QuickActionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ToolskuQuick"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "QuickActionActivity — temporary disabled")
        Toast.makeText(this, "Fitur Shortcut sedang dalam perbaikan", Toast.LENGTH_LONG).show()
        goHome()
    }

    private fun goHome() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "goHome failed", e)
            finish()
        }
    }
}
