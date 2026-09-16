package com.toolsku.app.automation

/**
 * Satu langkah otomasi.
 */
sealed class ActionStep {

    /** Buka App Info untuk package tertentu. */
    data class OpenAppInfo(val packageName: String) : ActionStep()

    /** Klik node berdasarkan daftar kandidat teks. */
    data class ClickByText(val texts: List<String>) : ActionStep()

    /** Klik node dengan safety — hindari dangerTexts. */
    data class ClickByTextSafe(
        val texts: List<String>,
        val dangerTexts: List<String>
    ) : ActionStep()

    /** Tekan tombol Back. */
    object Back : ActionStep()

    /** Tunggu (ms). */
    data class Wait(val millis: Long) : ActionStep()

    /** Cek apakah ada node dengan teks tertentu. */
    data class ExpectText(val texts: List<String>) : ActionStep()
}

/**
 * Task lengkap = kumpulan ActionStep untuk 1 app.
 *
 * @param packageName Package name app
 * @param appLabel Label/nama app (untuk overlay)
 * @param steps Daftar langkah otomasi
 * @param onComplete Callback opsional saat task selesai
 */
data class AutomationTask(
    val packageName: String,
    val appLabel: String,
    val steps: List<ActionStep>,
    val onComplete: (() -> Unit)? = null
)
