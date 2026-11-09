package com.example.dynamictheme

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.example.dynamictheme.theme.tokens.TokenThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        val repository = ServiceLocator.themeRepository

        // Load the cached theme before the first Activity is created so the first frame
        // already uses it (also after process death). Small local read, capped.
        val start = SystemClock.elapsedRealtime()
        val loaded = runBlocking {
            withTimeoutOrNull(CACHE_TIMEOUT_MS) { repository.init() }
        }
        Log.d("App", "cache init: ${SystemClock.elapsedRealtime() - start} ms, loaded=${loaded != null}, theme=${repository.theme.value.id}")

        // API 30+: override the XML tokens with the theme and keep them in sync.
        TokenThemeManager.install(this, repository.theme.value)
        appScope.launch {
            repository.theme.collect(TokenThemeManager::onThemeChanged)
        }
    }

    private companion object {
        const val CACHE_TIMEOUT_MS = 500L
    }
}
