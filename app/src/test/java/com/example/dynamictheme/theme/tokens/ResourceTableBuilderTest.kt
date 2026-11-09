package com.example.dynamictheme.theme.tokens

import com.example.dynamictheme.R
import com.example.dynamictheme.theme.DefaultTheme
import com.example.dynamictheme.theme.Palette
import com.example.dynamictheme.theme.tokens.ResourceTableBuilder.Entry
import com.example.dynamictheme.theme.tokens.ResourceTableBuilder.Value
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ResourceTableBuilderTest {

    private val color = ResourceTableBuilder.TYPE_INT_COLOR_ARGB8

    private fun sampleEntries() = listOf(
        Entry(0x7f050010, "token_primary", "color", Value(color, 0xFF0061A4.toInt()), Value(color, 0xFF9ECAFF.toInt())),
        Entry(0x7f050003, "token_on_primary", "color", Value(color, 0xFFFFFFFF.toInt()), Value(color, 0xFF003258.toInt())),
        Entry(0x7f060002, "token_corner_radius", "dimen", Value(ResourceTableBuilder.TYPE_DIMENSION, DimenEncoding.dp(12f))),
    )

    private fun ByteArray.le() = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)

    @Test
    fun `table header and package header are consistent`() {
        val bytes = ResourceTableBuilder.build("com.example.dynamictheme", sampleEntries())
        val buf = bytes.le()
        assertEquals(0x0002, buf.getShort(0).toInt()) // RES_TABLE_TYPE
        assertEquals(12, buf.getShort(2).toInt())
        assertEquals(bytes.size, buf.getInt(4)) // total size
        assertEquals(1, buf.getInt(8)) // package count

        val globalPoolSize = buf.getInt(12 + 4)
        val pkg = 12 + globalPoolSize
        assertEquals(0x0200, buf.getShort(pkg).toInt())
        assertEquals(bytes.size - pkg, buf.getInt(pkg + 4))
        assertEquals(0x7f, buf.getInt(pkg + 8))
        assertEquals(6, buf.getInt(pkg + 8 + 4 + 256 + 4)) // lastPublicType = max type id
        assertEquals(3, buf.getInt(pkg + 8 + 4 + 256 + 12)) // lastPublicKey = key count
    }

    @Test
    fun `every chunk size adds up exactly`() {
        val bytes = ResourceTableBuilder.build("com.example.dynamictheme", sampleEntries())
        val buf = bytes.le()
        val pkg = 12 + buf.getInt(16)
        var offset = pkg + 288 // walk the package body chunk by chunk
        val types = mutableListOf<Int>()
        while (offset < bytes.size) {
            types += buf.getShort(offset).toInt()
            val size = buf.getInt(offset + 4)
            assertTrue("chunk size must be 4-byte aligned", size % 4 == 0)
            offset += size
        }
        assertEquals(bytes.size, offset)
        // type strings, key strings, then per type: spec + default type (+ night type for colors)
        assertEquals(listOf(0x0001, 0x0001, 0x0202, 0x0201, 0x0201, 0x0202, 0x0201), types)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects entries from different packages`() {
        ResourceTableBuilder.build(
            "x",
            listOf(
                Entry(0x7f050001, "a", "color", Value(color, 0)),
                Entry(0x01050001, "b", "color", Value(color, 0)),
            ),
        )
    }

    @Test
    fun `dimen encoding round-trips`() {
        listOf(0f, 4f, 12f, 12.5f, 20f, 28f, 48f).forEach {
            assertEquals(it, DimenEncoding.decodeDp(DimenEncoding.dp(it)), 1f / 128f)
        }
        // unit dip (1) in the low nibble, radix 16p7 (1) in the next
        assertEquals(0x11, DimenEncoding.dp(12f) and 0xFF)
    }

    @Test
    fun `registry maps every palette field to a distinct color token`() {
        val ids = TokenRegistry.colorTokens.map { it.first }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(R.color.token_primary in ids)

        // A palette with all-distinct values: every field must be read by exactly one token.
        val p = Palette(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val values = TokenRegistry.colorTokens.map { (_, get) -> get(p) }.sorted()
        assertEquals((1..10).toList(), values)
        assertEquals(Palette::class.java.declaredFields.count { it.type == Int::class.javaPrimitiveType }, ids.size)
    }

    @Test
    fun `writes a sample table for manual aapt2 inspection`() {
        val entries = TokenRegistry.colorTokens.mapIndexed { i, (id, get) ->
            Entry(id, "token_$i", "color", Value(color, get(DefaultTheme.light)), Value(color, get(DefaultTheme.dark)))
        }
        File("build/token-table-sample.arsc").writeBytes(ResourceTableBuilder.build("com.example.dynamictheme", entries))
    }
}
