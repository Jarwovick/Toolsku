package com.toolsku.app.killer.step

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.toolsku.app.killer.action.EmptyAction
import com.toolsku.app.killer.action.ForceStopAction
import com.toolsku.app.killer.core.StepResult

/**
 * Step: Buka App Info + klik "Paksa berhenti".
 * Tiru dari `cd1` Baxa.
 */
class OpenAppInfoStep(
    private val context: Context,
    private val packageName: String
) : BaseStep() {

    companion object {
        private const val TAG = "OpenAppInfoStep"
    }

    private var forceStopBounds: Rect? = null

    init {
        currentPackage = packageName
        actions.add(EmptyAction(this))
        actions.add(ForceStopAction(this, this))
    }

    override fun execute(): StepResult {
        Log.i(TAG, "Opening App Info for $packageName")

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val isStopped = (appInfo.flags and ApplicationInfo.FLAG_STOPPED) != 0

            if (isStopped) {
                Log.d(TAG, "App already stopped — skip")
                return StepResult.skipTask()
            }

            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            context.startActivity(intent)

            StepResult.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
            StepResult.error()
        }
    }

    fun setForceStopBounds(rect: Rect) {
        this.forceStopBounds = rect
    }

    fun getForceStopBounds(): Rect? = forceStopBounds

    override fun getName(): String = "OpenAppInfoStep($packageName)"
}
