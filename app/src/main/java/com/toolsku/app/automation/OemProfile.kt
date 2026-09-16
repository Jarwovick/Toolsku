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

    /** Tombol "Paksa berhenti" di halaman App Info. */
    val forceStopButtonLabels = listOf(
        "Paksa berhenti",
        "Force stop",
        "强制停止",
        "強制停止",
    )

    /** Tombol konfirmasi di dialog force stop. */
    val forceStopConfirmLabels = listOf(
        "Paksa berhenti",
        "Force stop",
        "OK",
        "Ya",
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
        "Penyimpanan & cache",
        "Storage & cache",
        "Penggunaan penyimpanan",
        "Storage usage",
        "Penyimpanan",
        "Storage",
        "存储",
    )

    /** Tombol "Hapus cache" — yang akan DIKLIK. */
    val clearCacheLabels = listOf(
        "Hapus cache",
        "Clear cache",
        "清除缓存",
        "清除快取",
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
