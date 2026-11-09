package com.example.dynamictheme.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.dynamictheme.ServiceLocator
import com.example.dynamictheme.data.ThemeRepository
import com.example.dynamictheme.theme.AppTheme
import com.google.android.material.color.MaterialColors

/**
 * Extend this and call setContentView() in onCreate as usual.
 *
 * No colors are set in code: the views inflate with the overridden XML tokens
 * (TokenThemeManager), and a theme change recreates this Activity.
 */
abstract class BaseThemedActivity : AppCompatActivity() {

    protected val themeRepository: ThemeRepository get() = ServiceLocator.themeRepository
    protected val currentTheme: AppTheme get() = themeRepository.theme.value

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateSystemBarIcons()
        onThemeApplied(currentTheme)
    }

    /**
     * Light or dark status/nav bar icons, depending on the token colors behind them.
     * (windowLightStatusBar is a fixed boolean in XML, so this is the one thing needing code.)
     */
    private fun updateSystemBarIcons() {
        val statusBar = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
        val navBar = MaterialColors.getColor(this, android.R.attr.colorBackground, Color.WHITE)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = ColorUtils.calculateLuminance(statusBar) > 0.5
            isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(navBar) > 0.5
        }
    }

    /** Hook for screen-specific UI (e.g. showing the theme name), run on every (re)creation. */
    protected open fun onThemeApplied(theme: AppTheme) {}
}
