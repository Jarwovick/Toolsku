package com.toolsku.app.killer.action

import com.toolsku.app.killer.core.ActionStep
import com.toolsku.app.killer.core.StepResult

/**
 * Action placeholder — langsung sukses.
 * Tiru dari `xy1` Baxa.
 */
class EmptyAction(
    parentStep: ActionStep
) : BaseAction(parentStep) {

    override fun execute(): StepResult {
        return StepResult.success()
    }
}
