package com.toolsku.app.killer.step

import android.util.Log
import com.toolsku.app.killer.action.ClickOKAction
import com.toolsku.app.killer.action.EmptyAction
import com.toolsku.app.killer.core.StepResult

class ClickConfirmStep(
    private val openAppInfoStep: OpenAppInfoStep
) : BaseStep() {

    companion object {
        private const val TAG = "ClickConfirmStep"
    }

    init {
        currentPackage = openAppInfoStep.getPackage()
        actions.add(EmptyAction(this))
        actions.add(ClickOKAction(this, openAppInfoStep))
    }

    override fun execute(): StepResult {
        Log.d(TAG, "Waiting for confirm dialog...")
        return StepResult.waitEvent()
    }

    override fun getName(): String = "ClickConfirmStep"
}
