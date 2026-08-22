package com.patrykadamski.waterreminder

import android.content.SharedPreferences

/**
 * A user-defined "quick add" preset, e.g. "Szklanka" / 200ml.
 * [id] is a stable identity for editing/removing a specific entry from the
 * list in the UI - it has no meaning outside this device (not stored with
 * any water log, so existing WaterEntity rows are entirely unaffected by
 * adding, editing, or removing vessel sizes).
 */
data class VesselSize(
    val id: Long,
    val name: String,
    val amountMl: Int
)

object VesselSizePrefs {
    private const val KEY = "vessel_sizes"

    // Record/field separators unlikely to ever be typed by a user; names are
    // sanitized on save so they can never contain them, which keeps parsing
    // on load trivial and crash-free.
    private const val RECORD_SEPARATOR = ""
    private const val FIELD_SEPARATOR = ""

    // Kept short on purpose - the main screen also has a free-text "custom
    // amount" field now, so the default preset list doesn't need to cover
    // every vessel size, just the couple of quickest, most common taps.
    val DEFAULT_SIZES = listOf(
        VesselSize(id = 1L, name = "Szklanka", amountMl = 200),
        VesselSize(id = 2L, name = "Kubek", amountMl = 300)
    )

    fun load(prefs: SharedPreferences): List<VesselSize> {
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrEmpty()) return DEFAULT_SIZES

        val parsed = raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val parts = record.split(FIELD_SEPARATOR)
            if (parts.size != 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val amount = parts[2].toIntOrNull() ?: return@mapNotNull null
            if (amount <= 0) return@mapNotNull null
            VesselSize(id = id, name = parts[1], amountMl = amount)
        }
        return parsed.ifEmpty { DEFAULT_SIZES }
    }

    fun save(prefs: SharedPreferences, sizes: List<VesselSize>) {
        val raw = sizes.joinToString(RECORD_SEPARATOR) { size ->
            "${size.id}$FIELD_SEPARATOR${sanitize(size.name)}$FIELD_SEPARATOR${size.amountMl}"
        }
        prefs.edit().putString(KEY, raw).apply()
    }

    fun newId(): Long = System.currentTimeMillis()

    private fun sanitize(name: String): String =
        name.replace(RECORD_SEPARATOR, "").replace(FIELD_SEPARATOR, "").trim()
}
