package com.example.dynamictheme.theme

/** Parses "#RRGGBB" / "#AARRGGBB". Pure Kotlin so it runs in JVM unit tests. */
object HexColor {
    private val HEX = Regex("^[0-9a-fA-F]+$")

    fun parse(value: String?): Int? {
        val hex = value?.trim()?.removePrefix("#") ?: return null
        if (!HEX.matches(hex)) return null
        val argb = when (hex.length) {
            6 -> hex.toLong(16) or 0xFF000000
            8 -> hex.toLong(16)
            else -> return null
        }
        return argb.toInt()
    }
}

object ThemeMapper {
    private const val MAX_RADIUS_DP = 48f

    /**
     * Converts the API DTO into a usable theme.
     * Returns null when the theme is unusable (no valid `colors.light.primary`).
     * Any other missing or malformed field falls back to [DefaultTheme] per field.
     * A missing `colors.dark` section reuses the light palette.
     */
    fun toDomain(dto: ThemeDto): AppTheme? {
        val lightDto = dto.colors?.light ?: return null
        val lightPrimary = HexColor.parse(lightDto.primary) ?: return null
        val light = lightDto.toPalette(DefaultTheme.light, lightPrimary)

        val dark = dto.colors.dark
            ?.let { it.toPalette(DefaultTheme.dark, HexColor.parse(it.primary) ?: light.primary) }
            ?: light

        return AppTheme(
            id = dto.id ?: "remote",
            version = dto.version ?: 0,
            light = light,
            dark = dark,
            cornerRadiusDp = dto.shape?.cornerRadiusDp?.coerceIn(0f, MAX_RADIUS_DP)
                ?: DefaultTheme.CORNER_RADIUS_DP,
        )
    }

    private fun PaletteDto.toPalette(fallback: Palette, primary: Int) = Palette(
        primary = primary,
        onPrimary = HexColor.parse(onPrimary) ?: fallback.onPrimary,
        secondary = HexColor.parse(secondary) ?: fallback.secondary,
        onSecondary = HexColor.parse(onSecondary) ?: fallback.onSecondary,
        background = HexColor.parse(background) ?: fallback.background,
        onBackground = HexColor.parse(onBackground) ?: fallback.onBackground,
        surface = HexColor.parse(surface) ?: fallback.surface,
        onSurface = HexColor.parse(onSurface) ?: fallback.onSurface,
        error = HexColor.parse(error) ?: fallback.error,
        onError = HexColor.parse(onError) ?: fallback.onError,
    )
}
