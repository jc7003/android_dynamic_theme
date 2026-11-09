/*
 * Builds a minimal compiled resource table (resources.arsc) in memory.
 *
 * Approach follows Material Components' ColorResourcesTableCreator
 * (Apache License 2.0, https://github.com/material-components/material-components-android),
 * rewritten in Kotlin and extended with a night (uiMode) configuration and non-color values.
 */
package com.example.dynamictheme.theme.tokens

import java.io.ByteArrayOutputStream

/**
 * Pure Kotlin (no Android APIs), so it is unit-testable on the JVM.
 *
 * The table only contains the overridden entries, with the SAME resource ids as the app.
 * Loaded through a ResourcesLoader, its values win over the APK's for equally specific
 * configurations: the default config overrides `values/`, the night config overrides
 * `values-night/`.
 */
object ResourceTableBuilder {

    /** A compiled Res_value. */
    data class Value(val dataType: Int, val data: Int)

    data class Entry(
        val resId: Int,
        /** Entry name, e.g. "token_primary". */
        val name: String,
        /** Type name, e.g. "color" or "dimen". */
        val typeName: String,
        /** Value for the default configuration (values/). */
        val default: Value,
        /** Value for the night configuration (values-night/), or null if it doesn't vary. */
        val night: Value? = null,
    )

    // Res_value data types (ResourceTypes.h).
    const val TYPE_DIMENSION = 0x05
    const val TYPE_INT_COLOR_ARGB8 = 0x1c

    // Chunk types.
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_TABLE_TYPE = 0x0002
    private const val RES_TABLE_PACKAGE_TYPE = 0x0200
    private const val RES_TABLE_TYPE_TYPE = 0x0201
    private const val RES_TABLE_TYPE_SPEC_TYPE = 0x0202

    private const val UTF8_FLAG = 1 shl 8
    private const val NO_ENTRY = -1 // 0xFFFFFFFF
    private const val SPEC_PUBLIC = 0x40000000
    private const val CONFIG_UI_MODE = 0x1000
    private const val UI_MODE_NIGHT_YES = 0x20

    private const val CONFIG_SIZE = 64
    private const val PACKAGE_HEADER_SIZE = 288
    private const val TYPE_HEADER_SIZE = 20 + CONFIG_SIZE
    private const val ENTRY_SIZE = 16 // ResTable_entry (8) + Res_value (8)

    fun build(packageName: String, entries: List<Entry>): ByteArray {
        require(entries.isNotEmpty()) { "No entries to override" }
        val packageId = entries.first().resId ushr 24
        require(entries.all { it.resId ushr 24 == packageId }) { "All entries must be in one package" }
        require(entries.map { it.resId }.toSet().size == entries.size) { "Duplicate resource ids" }

        val keys = entries.map { it.name }
        val keyIndex = entries.withIndex().associate { (i, e) -> e.resId to i }
        val byType = entries.groupBy { typeId(it.resId) }.toSortedMap()

        val maxTypeId = byType.lastKey()
        val typeNames = (1..maxTypeId).map { id -> byType[id]?.first()?.typeName ?: "_unused$id" }

        val typeStrings = stringPool(typeNames)
        val keyStrings = stringPool(keys)
        val typeChunks = Buf().apply {
            byType.forEach { (typeId, typeEntries) ->
                bytes(typeSpec(typeId, typeEntries))
                bytes(type(typeId, typeEntries, keyIndex, night = false))
                if (typeEntries.any { it.night != null }) {
                    bytes(type(typeId, typeEntries.filter { it.night != null }, keyIndex, night = true))
                }
            }
        }.toByteArray()

        val packageBody = typeStrings + keyStrings + typeChunks
        val pkg = Buf().apply {
            u16(RES_TABLE_PACKAGE_TYPE); u16(PACKAGE_HEADER_SIZE); u32(PACKAGE_HEADER_SIZE + packageBody.size)
            u32(packageId)
            val name = packageName.take(127)
            name.forEach { u16(it.code) }
            repeat(128 - name.length) { u16(0) }
            u32(PACKAGE_HEADER_SIZE) // typeStrings offset
            u32(typeNames.size) // lastPublicType
            u32(PACKAGE_HEADER_SIZE + typeStrings.size) // keyStrings offset
            u32(keys.size) // lastPublicKey
            u32(0) // typeIdOffset
            bytes(packageBody)
        }.toByteArray()

        val globalStrings = stringPool(emptyList())
        return Buf().apply {
            u16(RES_TABLE_TYPE); u16(12); u32(12 + globalStrings.size + pkg.size)
            u32(1) // package count
            bytes(globalStrings)
            bytes(pkg)
        }.toByteArray()
    }

    private fun typeId(resId: Int) = (resId ushr 16) and 0xFF
    private fun entryId(resId: Int) = resId and 0xFFFF
    private fun entryCount(entries: List<Entry>) = entries.maxOf { entryId(it.resId) } + 1

    private fun typeSpec(typeId: Int, entries: List<Entry>): ByteArray {
        val count = entryCount(entries)
        val flags = IntArray(count)
        entries.forEach { flags[entryId(it.resId)] = SPEC_PUBLIC or (if (it.night != null) CONFIG_UI_MODE else 0) }
        return Buf().apply {
            u16(RES_TABLE_TYPE_SPEC_TYPE); u16(16); u32(16 + 4 * count)
            u8(typeId); u8(0); u16(0)
            u32(count)
            flags.forEach { u32(it) }
        }.toByteArray()
    }

    private fun type(typeId: Int, entries: List<Entry>, keyIndex: Map<Int, Int>, night: Boolean): ByteArray {
        val count = entryCount(entries)
        val offsets = IntArray(count) { NO_ENTRY }
        val data = Buf()
        entries.sortedBy { entryId(it.resId) }.forEach { e ->
            val value = if (night) e.night!! else e.default
            offsets[entryId(e.resId)] = data.size
            data.u16(8); data.u16(0); data.u32(keyIndex.getValue(e.resId)) // ResTable_entry
            data.u16(8); data.u8(0); data.u8(value.dataType); data.u32(value.data) // Res_value
        }
        val entriesStart = TYPE_HEADER_SIZE + 4 * count
        return Buf().apply {
            u16(RES_TABLE_TYPE_TYPE); u16(TYPE_HEADER_SIZE); u32(entriesStart + data.size)
            u8(typeId); u8(0); u16(0)
            u32(count)
            u32(entriesStart)
            bytes(config(night))
            offsets.forEach { u32(it) }
            bytes(data.toByteArray())
        }.toByteArray()
    }

    /** ResTable_config: all zero (matches any device) except size and, for night, uiMode. */
    private fun config(night: Boolean) = ByteArray(CONFIG_SIZE).also {
        it[0] = CONFIG_SIZE.toByte()
        if (night) it[UI_MODE_OFFSET] = UI_MODE_NIGHT_YES.toByte()
    }

    private const val UI_MODE_OFFSET = 29

    private fun stringPool(strings: List<String>): ByteArray {
        val data = Buf()
        val offsets = strings.map { s ->
            val offset = data.size
            val utf8 = s.toByteArray(Charsets.UTF_8)
            data.utf8Length(s.length)
            data.utf8Length(utf8.size)
            data.bytes(utf8)
            data.u8(0)
            offset
        }
        while (data.size % 4 != 0) data.u8(0)
        val headerSize = 28
        val stringsStart = if (strings.isEmpty()) 0 else headerSize + 4 * strings.size
        return Buf().apply {
            u16(RES_STRING_POOL_TYPE); u16(headerSize); u32(headerSize + 4 * strings.size + data.size)
            u32(strings.size) // stringCount
            u32(0) // styleCount
            u32(UTF8_FLAG)
            u32(stringsStart)
            u32(0) // stylesStart
            offsets.forEach { u32(it) }
            bytes(data.toByteArray())
        }.toByteArray()
    }

    /** Little-endian writer. */
    private class Buf {
        private val out = ByteArrayOutputStream()
        val size: Int get() = out.size()
        fun u8(v: Int) = out.write(v and 0xFF)
        fun u16(v: Int) { u8(v); u8(v ushr 8) }
        fun u32(v: Int) { u16(v); u16(v ushr 16) }
        fun bytes(b: ByteArray) = out.write(b)
        fun utf8Length(len: Int) {
            if (len > 0x7F) { u8(((len shr 8) and 0x7F) or 0x80); u8(len) } else u8(len)
        }
        fun toByteArray(): ByteArray = out.toByteArray()
    }
}

/** Encodes dp values as Res_value TYPE_DIMENSION data (radix 16p7, unit dip). */
object DimenEncoding {
    private const val COMPLEX_UNIT_DIP = 1
    private const val COMPLEX_RADIX_16P7 = 1
    private const val RADIX_SHIFT = 4
    private const val MANTISSA_SHIFT = 8

    fun dp(value: Float): Int {
        require(value >= 0f && value < 65536f) { "dp out of range: $value" }
        val mantissa = Math.round(value * 128f) // 7 fractional bits
        return (mantissa shl MANTISSA_SHIFT) or (COMPLEX_RADIX_16P7 shl RADIX_SHIFT) or COMPLEX_UNIT_DIP
    }

    /** Inverse of [dp], same math as android.util.TypedValue.complexToFloat. For tests. */
    fun decodeDp(complex: Int): Float = (complex shr MANTISSA_SHIFT) / 128f
}
