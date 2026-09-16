package com.toolsku.app.automation

sealed class ActionStep {

    data class OpenAppInfo(val packageName: String) : ActionStep()

    data class ClickByText(val texts: List<String>) : ActionStep()

    data class ClickByTextSafe(
        val texts: List<String>,
        val dangerTexts: List<String>
    ) : ActionStep()

    object Back : ActionStep()

    data class Wait(val millis: Long) : ActionStep()

    data class ExpectText(val texts: List<String>) : ActionStep()
}

/**
 * Task lengkap = kumpulan ActionStep untuk 1 app.
 * @param packageName Package name
 * @param appLabel Label/nama app (untuk tampilan overlay)
 */
data class AutomationTask(
    val packageName: String,
    val appLabel: String,
    val steps: List<ActionStep>,
    val onComplete: (() -> Unit)? = null
)
