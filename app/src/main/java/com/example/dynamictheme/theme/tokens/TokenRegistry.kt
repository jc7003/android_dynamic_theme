package com.example.dynamictheme.theme.tokens

import android.content.res.Resources
import com.example.dynamictheme.R
import com.example.dynamictheme.theme.AppTheme
import com.example.dynamictheme.theme.Palette
import com.example.dynamictheme.theme.tokens.ResourceTableBuilder.Entry
import com.example.dynamictheme.theme.tokens.ResourceTableBuilder.Value

/**
 * Explicit whitelist: JSON theme field → XML token resource.
 * Adding a token = add it to tokens.xml, to Palette/PaletteDto, and one line here.
 */
object TokenRegistry {

    val colorTokens: List<Pair<Int, (Palette) -> Int>> = listOf(
        R.color.token_primary to Palette::primary,
        R.color.token_on_primary to Palette::onPrimary,
        R.color.token_secondary to Palette::secondary,
        R.color.token_on_secondary to Palette::onSecondary,
        R.color.token_background to Palette::background,
        R.color.token_on_background to Palette::onBackground,
        R.color.token_surface to Palette::surface,
        R.color.token_on_surface to Palette::onSurface,
        R.color.token_error to Palette::error,
        R.color.token_on_error to Palette::onError,
    )

    val cornerRadiusToken: Int = R.dimen.token_corner_radius

    /** Builds the override entries: colors get a light (default) and a dark (night) value. */
    fun entries(res: Resources, theme: AppTheme): List<Entry> {
        val colors = colorTokens.map { (id, color) ->
            entry(
                res, id,
                default = Value(ResourceTableBuilder.TYPE_INT_COLOR_ARGB8, color(theme.light)),
                night = Value(ResourceTableBuilder.TYPE_INT_COLOR_ARGB8, color(theme.dark)),
            )
        }
        val radius = entry(
            res, cornerRadiusToken,
            default = Value(ResourceTableBuilder.TYPE_DIMENSION, DimenEncoding.dp(theme.cornerRadiusDp)),
            night = null,
        )
        return colors + radius
    }

    private fun entry(res: Resources, id: Int, default: Value, night: Value?) = Entry(
        resId = id,
        name = res.getResourceEntryName(id),
        typeName = res.getResourceTypeName(id),
        default = default,
        night = night,
    )
}
