package com.toolsku.app.automation

import android.os.Build

/**
 * Profil OEM untuk kalibrasi otomasi.
 * Label tombol berbeda antar versi Android/OEM.
 *
 * Terkalibrasi dari ColorOS 15 (Oppo Find N3) & ColorOS 6/7 (Realme 3 Pro).
 */
object OemProfile {

    // ==== FORCE STOP ====

    /** Tombol "Paksa berhenti" di halaman App Info (atas). */
    val forceStopButtonLabels = listOf(
        "Paksa berhenti",      // ID (ColorOS 15, Realme UI 1.0)
        "Force stop",           // EN
        "强制停止",              // ZH
        "強制停止",              // ZH-TW
    )

    /** Tombol konfirmasi di dialog force stop. */
    val forceStopConfirmLabels = listOf(
        "Paksa berhenti",      // ID — di dialog konfirmasi
        "Force stop",           // EN
        "OK",                   // fallback umum
        "Ya",                   // fallback
    )

    /** Tombol cancel — HARUS DIHINDARI. */
    val cancelLabels = listOf(
        "Batalkan",
        "Batal",
        "Cancel",
        "取消",
    )

    // ==== CLEAR CACHE ====

    /** Menu "Penyimpanan" / "Penggunaan penyimpanan". */
    val storageMenuLabels = listOf(
        "Penyimpanan & cache",       // ID (format baru)
        "Storage & cache",           // EN
        "Penggunaan penyimpanan",    // ID (ColorOS 15 — dari screenshot)
        "Storage usage",             // EN
        "Penyimpanan",               // ID (singkat)
        "Storage",                   // EN (singkat)
        "存储",                      // ZH
    )

    /** Tombol "Hapus cache" — yang akan DIKLIK. */
    val clearCacheLabels = listOf(
        "Hapus cache",         // ID
        "Clear cache",          // EN
        "清除缓存",             // ZH
        "清除快取",             // ZH-TW
    )

    /** Tombol "Hapus data" — HARUS DIHINDARI (danger). */
    val clearDataLabels = listOf(
        "Hapus data",
        "Hapus penyimpanan",
        "Clear data",
        "Clear storage",
        "清除数据",
        "清除儲存空間",
    )

    // ==== DETEKSI OEM ====

    /** Nama paket Settings (bisa beda per OEM). */
    val settingsPackages = setOf(
        "com.android.settings",
        "com.coloros.settings",
        "com.oppo.settings",
        "com.oneplus.settings",
    )

    /** Cek apakah device ini ColorOS (Oppo/Realme/OnePlus). */
    fun isColorOS(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("oppo") ||
               manufacturer.contains("realme") ||
               manufacturer.contains("oneplus")
    }
}
