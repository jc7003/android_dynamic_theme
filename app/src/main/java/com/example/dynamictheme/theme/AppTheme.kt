package com.example.dynamictheme.theme

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---------- Wire format (what the API returns) ----------
// @Serializable: JSON <-> object (Retrofit response, DataStore cache).

@Serializable
data class ThemeDto(
    val id: String? = null,
    val version: Int? = null,
    val colors: ColorsDto? = null,
    val shape: ShapeDto? = null,
)

@Serializable
data class ColorsDto(
    val light: PaletteDto? = null,
    val dark: PaletteDto? = null,
)

@Serializable
data class PaletteDto(
    val primary: String? = null,
    val onPrimary: String? = null,
    val secondary: String? = null,
    val onSecondary: String? = null,
    val background: String? = null,
    val onBackground: String? = null,
    val surface: String? = null,
    val onSurface: String? = null,
    val error: String? = null,
    val onError: String? = null,
)

@Serializable
data class ShapeDto(
    val cornerRadiusDp: Float? = null,
)

/**
 * Shared JSON config: tolerant of extra/missing fields so the API can evolve.
 * Missing keys decode to null because every DTO field defaults to `= null`.
 */
val AppJson = Json {
    ignoreUnknownKeys = true
}

// ---------- Resolved domain model (what the UI uses) ----------

/** All values are ARGB color ints, ready for Android APIs. */
data class Palette(
    val primary: Int,
    val onPrimary: Int,
    val secondary: Int,
    val onSecondary: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val error: Int,
    val onError: Int,
)

data class AppTheme(
    val id: String,
    val version: Int,
    val light: Palette,
    val dark: Palette,
    val cornerRadiusDp: Float,
)

/** Built-in fallback. Keep in sync with res/values/colors.xml. */
object DefaultTheme {
    const val ID = "default"
    const val CORNER_RADIUS_DP = 20f

    val light = Palette(
        primary = 0xFF6750A4.toInt(),
        onPrimary = 0xFFFFFFFF.toInt(),
        secondary = 0xFF625B71.toInt(),
        onSecondary = 0xFFFFFFFF.toInt(),
        background = 0xFFFFFBFE.toInt(),
        onBackground = 0xFF1C1B1F.toInt(),
        surface = 0xFFF3EDF7.toInt(),
        onSurface = 0xFF1C1B1F.toInt(),
        error = 0xFFB3261E.toInt(),
        onError = 0xFFFFFFFF.toInt(),
    )

    val dark = Palette(
        primary = 0xFFD0BCFF.toInt(),
        onPrimary = 0xFF381E72.toInt(),
        secondary = 0xFFCCC2DC.toInt(),
        onSecondary = 0xFF332D41.toInt(),
        background = 0xFF1C1B1F.toInt(),
        onBackground = 0xFFE6E1E5.toInt(),
        surface = 0xFF2B2930.toInt(),
        onSurface = 0xFFE6E1E5.toInt(),
        error = 0xFFF2B8B5.toInt(),
        onError = 0xFF601410.toInt(),
    )

    val theme = AppTheme(ID, 0, light, dark, CORNER_RADIUS_DP)
}
