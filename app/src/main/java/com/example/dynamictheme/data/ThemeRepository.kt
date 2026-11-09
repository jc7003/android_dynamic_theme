package com.example.dynamictheme.data

import com.example.dynamictheme.theme.AppTheme
import com.example.dynamictheme.theme.DefaultTheme
import com.example.dynamictheme.theme.ThemeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InvalidThemeException(message: String) : Exception(message)

/**
 * Single source of truth for the current theme.
 * Order: built-in default → cached theme ([init]) → fresh API theme ([refresh]).
 */
class ThemeRepository(
    private val source: ThemeSource,
    private val store: ThemeStore,
) {
    private val _theme = MutableStateFlow(DefaultTheme.theme)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    /** Emits the cached theme, if there is a valid one. */
    suspend fun init() {
        store.load()?.let(ThemeMapper::toDomain)?.let { _theme.value = it }
    }

    /**
     * Fetches a theme from the API. On success it is cached, then emitted.
     * On failure the current theme stays as is.
     */
    suspend fun refresh(name: String? = null): Result<AppTheme> {
        return try {
            val dto = source.fetch(name)
            val theme = ThemeMapper.toDomain(dto)
                ?: return Result.failure(
                    InvalidThemeException("Theme JSON has no valid colors.light.primary"),
                )
            // Cache BEFORE emitting: collectors may react synchronously (e.g. recreate()
            // the Activity whose scope runs this call), which would cancel a later save.
            try {
                store.save(dto)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A cache write failure is not fatal; still apply the theme.
            }
            _theme.value = theme
            Result.success(theme)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
