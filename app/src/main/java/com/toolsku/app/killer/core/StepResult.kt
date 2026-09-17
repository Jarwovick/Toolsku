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
        const val CODE_SKIP_TASK = 3
        const val CODE_KEEP_STAGE = 5
        const val CODE_REPEAT_TASK = 11

        fun success(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun keepStage(): StepResult = StepResult(CODE_KEEP_STAGE)

        fun skipTask(): StepResult {
            return StepResult(CODE_SKIP_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun repeatTask(): StepResult {
            return StepResult(CODE_REPEAT_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        fun error(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isError = true
            }
        }
    }

    fun isKeepStage(): Boolean = code == CODE_KEEP_STAGE
    fun isSkipTask(): Boolean = code == CODE_SKIP_TASK
    fun isRepeatTask(): Boolean = code == CODE_REPEAT_TASK

    override fun toString(): String {
        return when (code) {
            CODE_DEFAULT -> if (isComplete) "completed" else "default"
            CODE_KEEP_STAGE -> "keepStage"
            CODE_SKIP_TASK -> "skipTask"
            CODE_REPEAT_TASK -> "repeatTask"
            else -> "unknown($code)"
        }
    }
}
