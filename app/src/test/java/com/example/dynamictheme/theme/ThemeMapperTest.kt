package com.example.dynamictheme.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeMapperTest {

    private fun parse(json: String): AppTheme? =
        ThemeMapper.toDomain(AppJson.decodeFromString(ThemeDto.serializer(), json))

    @Test
    fun `hex parser handles rgb, argb and garbage`() {
        assertEquals(0xFF0061A4.toInt(), HexColor.parse("#0061A4"))
        assertEquals(0x800061A4.toInt(), HexColor.parse("#800061A4"))
        assertEquals(0xFFABCDEF.toInt(), HexColor.parse(" abcdef "))
        assertNull(HexColor.parse("#12345"))
        assertNull(HexColor.parse("not-a-color"))
        assertNull(HexColor.parse("#-12345"))
        assertNull(HexColor.parse(null))
    }

    @Test
    fun `full theme parses`() {
        val theme = parse(
            """
            {"id":"ocean","version":3,
             "colors":{"light":{"primary":"#0061A4","onPrimary":"#FFFFFF","surface":"#E3F2FD"},
                       "dark":{"primary":"#9ECAFF"}},
             "shape":{"cornerRadiusDp":12},
             "someFutureField":true}
            """,
        )!!
        assertEquals("ocean", theme.id)
        assertEquals(3, theme.version)
        assertEquals(0xFF0061A4.toInt(), theme.light.primary)
        assertEquals(0xFFE3F2FD.toInt(), theme.light.surface)
        assertEquals(0xFF9ECAFF.toInt(), theme.dark.primary)
        assertEquals(12f, theme.cornerRadiusDp)
    }

    @Test
    fun `missing optional fields fall back to defaults`() {
        val theme = parse("""{"colors":{"light":{"primary":"#112233"}}}""")!!
        assertEquals("remote", theme.id)
        assertEquals(0, theme.version)
        assertEquals(DefaultTheme.light.onPrimary, theme.light.onPrimary)
        assertEquals(DefaultTheme.light.error, theme.light.error)
        assertEquals(DefaultTheme.CORNER_RADIUS_DP, theme.cornerRadiusDp)
    }

    @Test
    fun `bad hex in optional field falls back per field`() {
        val theme = parse("""{"colors":{"light":{"primary":"#112233","secondary":"blue"}}}""")!!
        assertEquals(DefaultTheme.light.secondary, theme.light.secondary)
    }

    @Test
    fun `missing or invalid light primary rejects the theme`() {
        assertNull(parse("""{"colors":{"light":{"primary":"not-a-color"}}}"""))
        assertNull(parse("""{"colors":{"dark":{"primary":"#112233"}}}"""))
        assertNull(parse("""{}"""))
    }

    @Test
    fun `missing dark section reuses light palette`() {
        val theme = parse("""{"colors":{"light":{"primary":"#C2410C"}}}""")!!
        assertEquals(theme.light, theme.dark)
    }

    @Test
    fun `dark section without primary inherits light primary`() {
        val theme = parse("""{"colors":{"light":{"primary":"#C2410C"},"dark":{"surface":"#000000"}}}""")!!
        assertEquals(theme.light.primary, theme.dark.primary)
        assertEquals(0xFF000000.toInt(), theme.dark.surface)
        assertEquals(DefaultTheme.dark.onSurface, theme.dark.onSurface)
    }

    @Test
    fun `corner radius is clamped`() {
        assertEquals(48f, parse("""{"colors":{"light":{"primary":"#000000"}},"shape":{"cornerRadiusDp":999}}""")!!.cornerRadiusDp)
        assertEquals(0f, parse("""{"colors":{"light":{"primary":"#000000"}},"shape":{"cornerRadiusDp":-5}}""")!!.cornerRadiusDp)
    }
}
