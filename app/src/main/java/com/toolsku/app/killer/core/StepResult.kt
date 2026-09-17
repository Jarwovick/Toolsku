package com.toolsku.app.killer.core

/**
 * Hasil eksekusi satu step / action.
 * Tiru dari `ho2` Baxa.
 */
class StepResult(
    val code: Int = CODE_DEFAULT,
    var isComplete: Boolean = false,
    var isError: Boolean = false,
    var isSuccess: Boolean = false
) {
    companion object {
        const val CODE_DEFAULT = 0
        const val CODE_NEED_REPEAT = 2
        const val CODE_SKIP_TASK = 3
        const val CODE_KEEP_STAGE = 5
        const val CODE_FORCE_COMPLETE = 6
        const val CODE_REPEAT_STAGE = 7
        const val CODE_RESTART_STAGE = 8
        const val CODE_WAIT = 10
        const val CODE_REPEAT_TASK = 11

        fun success(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun keepStage(): StepResult {
            return StepResult(CODE_KEEP_STAGE)
        }

        fun skipTask(): StepResult {
            return StepResult(CODE_SKIP_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun repeatStage(): StepResult = StepResult(CODE_REPEAT_STAGE)

        fun restartStage(): StepResult = StepResult(CODE_RESTART_STAGE)

        fun repeatTask(): StepResult {
            return StepResult(CODE_REPEAT_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun waitEvent(): StepResult = StepResult(CODE_WAIT)

        fun error(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isError = true
            }
        }

        fun forceComplete(): StepResult {
            return StepResult(CODE_FORCE_COMPLETE).apply {
                isComplete = true
                isSuccess = true
            }
        }
    }

    fun isKeepStage(): Boolean = code == CODE_KEEP_STAGE
    fun isSkipTask(): Boolean = code == CODE_SKIP_TASK
    fun isRepeatStage(): Boolean = code == CODE_REPEAT_STAGE
    fun isRepeatTask(): Boolean = code == CODE_REPEAT_TASK

    override fun toString(): String {
        return when (code) {
            CODE_DEFAULT -> if (isComplete) "completed" else "default"
            CODE_KEEP_STAGE -> "keepStage"
            CODE_SKIP_TASK -> "skipTask"
            CODE_REPEAT_STAGE -> "repeatStage"
            CODE_RESTART_STAGE -> "restartStage"
            CODE_REPEAT_TASK -> "repeatTask"
            CODE_WAIT -> "waitEvent"
            CODE_FORCE_COMPLETE -> "forceComplete"
            else -> "unknown($code)"
        }
    }
}
