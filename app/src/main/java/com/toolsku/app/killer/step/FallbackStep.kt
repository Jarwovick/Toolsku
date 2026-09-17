package com.toolsku.app.killer.step

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.toolsku.app.killer.action.EmptyAction
import com.toolsku.app.killer.core.StepResult

/**
 * Step: Fallback — buka Settings.
 * Tiru dari `yy1` Baxa.
 */
class FallbackStep(
    private val context: Context,
    private val packageName: String
) : BaseStep() {

    companion object {
        private const val TAG = "FallbackStep"
    }

    init {
        currentPackage = packageName
        actions.add(EmptyAction(this))
    }

    override fun execute(): StepResult {
        Log.i(TAG, "Fallback: opening Settings")

        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)
            StepResult.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fallback failed", e)
            StepResult.error()
        }
    }

    override fun getName(): String = "FallbackStep($packageName)"
}
