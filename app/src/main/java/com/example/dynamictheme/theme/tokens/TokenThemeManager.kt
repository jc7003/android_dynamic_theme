package com.example.dynamictheme.theme.tokens

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.dynamictheme.theme.AppTheme
import com.example.dynamictheme.theme.DefaultTheme
import java.util.Collections
import java.util.WeakHashMap

/**
 * Applies a JSON theme by overriding the XML token resources, then recreating the screens.
 *
 * - API 30+: every Activity gets the override in onActivityPreCreated, before setContentView,
 *   so layouts, styles, selectors and theme attrs all resolve to the JSON values.
 * - Resources are read at inflate time, so a theme change recreates every live Activity.
 * - Below API 30 there is no ResourcesLoader: the XML default tokens are shown.
 */
object TokenThemeManager {
    private const val TAG = "TokenThemeManager"

    /** Whether this device can override the tokens (ResourcesLoader, API 30+). */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private lateinit var app: Application
    private var appliedTheme: AppTheme? = null
    private val liveActivities: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())

    /** Call once in Application.onCreate, after the cached theme is loaded. */
    fun install(app: Application, theme: AppTheme) {
        this.app = app
        app.registerActivityLifecycleCallbacks(Callbacks)
        appliedTheme = theme
        if (isSupported) override(theme)
    }

    /** Call whenever the repository emits a theme. Recreates screens if the theme changed. */
    fun onThemeChanged(theme: AppTheme) {
        if (!::app.isInitialized || theme == appliedTheme) return
        appliedTheme = theme
        if (isSupported) override(theme)
        liveActivities.toList().forEach { activity ->
            if (isSupported) TokenOverride.applyTo(activity.resources)
            activity.recreate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun override(theme: AppTheme) {
        try {
            if (theme.id == DefaultTheme.ID) {
                TokenOverride.clear() // the XML defaults already are the default theme
            } else {
                val entries = TokenRegistry.entries(app.resources, theme)
                TokenOverride.install(ResourceTableBuilder.build(app.packageName, entries))
            }
            TokenOverride.applyTo(app.resources)
        } catch (e: Exception) {
            Log.w(TAG, "Token override failed, keeping the current tokens", e)
        }
    }

    private object Callbacks : Application.ActivityLifecycleCallbacks {
        // API 29+ only; this runs before Activity.onCreate, i.e. before any inflation.
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (isSupported) TokenOverride.applyTo(activity.resources)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            liveActivities += activity
        }

        override fun onActivityDestroyed(activity: Activity) {
            liveActivities -= activity
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
