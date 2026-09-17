package com.toolsku.app.killer.core

/**
 * Hasil eksekusi satu step / action.
 * Tiru dari `ho2` Baxa.
 *
 * State codes:
 * - 0  = default (belum selesai)
 * - 2  = need repeat
 * - 3  = skipTask
 * - 5  = keepStage
 * - 6  = force completed
 * - 7  = repeatStage
 * - 8  = restartStage
 * - 10 = wait (tunggu event)
 * - 11 = repeatTask
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

        /**
         * Sukses — step selesai, lanjut step berikutnya.
         */
        fun success(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isSuccess = true
            }
        }

        /**
         * Ulangi step ini — tunggu event lagi.
         */
        fun keepStage(): StepResult {
            return StepResult(CODE_KEEP_STAGE)
        }

        /**
         * Skip task — app tidak perlu diproses.
         */
        fun skipTask(): StepResult {
            return StepResult(CODE_SKIP_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        /**
         * Ulangi step ini.
         */
        fun repeatStage(): StepResult {
            return StepResult(CODE_REPEAT_STAGE)
        }

        /**
         * Restart dari step pertama.
         */
        fun restartStage(): StepResult {
            return StepResult(CODE_RESTART_STAGE)
        }

        /**
         * Ulangi task.
         */
        fun repeatTask(): StepResult {
            return StepResult(CODE_REPEAT_TASK).apply {
                isComplete = true
                isSuccess = true
            }
        }

        /**
         * Tunggu event.
         */
        fun waitEvent(): StepResult {
            return StepResult(CODE_WAIT)
        }

        /**
         * Error — task gagal.
         */
        fun error(): StepResult {
            return StepResult(CODE_DEFAULT).apply {
                isComplete = true
                isError = true
            }
        }

        /**
         * Force complete — abaikan step berikutnya.
         */
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
