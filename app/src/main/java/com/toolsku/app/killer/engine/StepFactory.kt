package com.toolsku.app.killer.engine

import android.content.Context
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.step.ClickConfirmStep
import com.toolsku.app.killer.step.FallbackStep
import com.toolsku.app.killer.step.OpenAppInfoStep

/**
 * Factory untuk membuat step-step kill.
 * Urutan step: OpenAppInfo → ClickConfirm.
 *
 * Kalau ada yang gagal, FallbackStep sebagai cadangan.
 */
object StepFactory {

    /**
     * Buat daftar step untuk kill 1 app.
     *
     * Alur:
     * 1. OpenAppInfoStep — buka App Info + klik "Paksa berhenti"
     * 2. ClickConfirmStep — klik "OK" di dialog
     */
    fun createKillSteps(context: Context, packageName: String): List<ActionStep> {
        val openStep = OpenAppInfoStep(context, packageName)
        val confirmStep = ClickConfirmStep(openStep)
        
        return listOf(
            openStep,
            confirmStep
        )
    }

    /**
     * Buat step fallback — buka Settings.
     * Dipakai kalau app tidak bisa di-kill.
     */
    fun createFallbackStep(context: Context, packageName: String): ActionStep {
        return FallbackStep(context, packageName)
    }
}
