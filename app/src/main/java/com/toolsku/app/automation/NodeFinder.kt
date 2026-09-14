package com.toolsku.app.automation

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Helper untuk mencari node di Accessibility tree.
 * Aman dari klik tombol berbahaya (Hapus data, Uninstall, dll).
 */
object NodeFinder {

    /**
     * Cari node dari root berdasarkan daftar kandidat teks.
     * Return node pertama yang match.
     */
    fun findByText(root: AccessibilityNodeInfo?, texts: List<String>): AccessibilityNodeInfo? {
        if (root == null) return null

        // Coba cari langsung dari root
        for (text in texts) {
            val found = root.findAccessibilityNodeInfosByText(text)
            if (found != null && found.isNotEmpty()) {
                // Pastikan yang diklik adalah node leaf (bukan container)
                for (node in found) {
                    if (isClickable(node) || findClickableParent(node) != null) {
                        return node
                    }
                }
                return found[0]
            }
        }
        return null
    }

    /**
     * Cari node yang teksnya TIDAK termasuk danger.
     * Return null kalau semua match adalah danger.
     */
    fun findByTextSafe(
        root: AccessibilityNodeInfo?,
        texts: List<String>,
        dangerTexts: List<String>
    ): AccessibilityNodeInfo? {
        if (root == null) return null

        for (text in texts) {
            val found = root.findAccessibilityNodeInfosByText(text)
            if (found == null) continue

            for (node in found) {
                val nodeText = extractText(node) ?: continue

                // Skip kalau node text mengandung danger
                val isDanger = dangerTexts.any { danger ->
                    nodeText.contains(danger, ignoreCase = true)
                }
                if (isDanger) continue

                // Skip kalau ini node yang sama dengan "Hapus data"
                if (nodeText.contains("data", ignoreCase = true)) continue

                if (isClickable(node) || findClickableParent(node) != null) {
                    return node
                }
            }
        }
        return null
    }

    /**
     * Cari parent yang clickable.
     */
    fun findClickableParent(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    /**
     * Cek apakah node clickable.
     */
    private fun isClickable(node: AccessibilityNodeInfo?): Boolean {
        return node?.isClickable == true
    }

    /**
     * Ambil teks dari node (text atau contentDescription).
     */
    fun extractText(node: AccessibilityNodeInfo): String? {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        return text ?: desc
    }

    /**
     * Dump semua teks node di tree (untuk debug).
     */
    fun dumpTexts(root: AccessibilityNodeInfo?, depth: Int = 0): String {
        if (root == null) return ""
        val sb = StringBuilder()
        val text = extractText(root)
        if (!text.isNullOrBlank()) {
            sb.append("  ".repeat(depth)).append("- ").append(text).append("\n")
        }
        for (i in 0 until root.childCount) {
            sb.append(dumpTexts(root.getChild(i), depth + 1))
        }
        return sb.toString()
    }
}
