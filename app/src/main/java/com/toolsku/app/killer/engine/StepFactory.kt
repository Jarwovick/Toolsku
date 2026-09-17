package com.toolsku.app.killer.engine

import android.content.Context
import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.step.ClickConfirmStep
import com.toolsku.app.killer.step.OpenAppInfoStep

/**
 * Factory untuk membuat step-step kill.
 *
 * Alur:
 * 1. OpenAppInfoStep — buka App Info + klik "Paksa berhenti"
 * 2. ClickConfirmStep — klik "OK" di dialog
 */
object StepFactory {

    fun createKillSteps(context: Context, packageName: String): List<ActionStep> {
        val openStep = OpenAppInfoStep(context, packageName)
        val confirmStep = ClickConfirmStep(openStep)

        return listOf(
            openStep,
            confirmStep
        )
    }
}
