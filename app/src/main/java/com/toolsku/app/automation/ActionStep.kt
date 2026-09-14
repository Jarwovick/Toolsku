package com.toolsku.app.automation

/**
 * Satu langkah otomasi.
 * Setiap Task terdiri dari beberapa ActionStep yang dieksekusi berurutan.
 */
sealed class ActionStep {

    /** Buka App Info untuk package tertentu. */
    data class OpenAppInfo(val packageName: String) : ActionStep()

    /** Klik node berdasarkan daftar kandidat teks (match salah satu). */
    data class ClickByText(val texts: List<String>) : ActionStep()

    /** Klik node berdasarkan daftar kandidat teks, tapi JANGAN klik kalau match dengan danger. */
    data class ClickByTextSafe(
        val texts: List<String>,
        val dangerTexts: List<String>
    ) : ActionStep()

    /** Tekan tombol Back. */
    object Back : ActionStep()

    /** Tunggu (ms). */
    data class Wait(val millis: Long) : ActionStep()

    /** Cek apakah ada node dengan teks tertentu (untuk conditional flow). */
    data class ExpectText(val texts: List<String>) : ActionStep()
}

/**
 * Task lengkap = kumpulan ActionStep untuk 1 app.
 */
data class AutomationTask(
    val packageName: String,
    val steps: List<ActionStep>,
    val onComplete: (() -> Unit)? = null
)
