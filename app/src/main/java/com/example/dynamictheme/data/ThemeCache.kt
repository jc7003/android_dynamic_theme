package com.example.dynamictheme.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dynamictheme.theme.AppJson
import com.example.dynamictheme.theme.ThemeDto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import java.io.IOException

/** Persists the last good theme. Abstracted so the repository can be unit-tested. */
interface ThemeStore {
    suspend fun load(): ThemeDto?
    suspend fun save(theme: ThemeDto)
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_cache")

class ThemeCache(context: Context) : ThemeStore {
    private val dataStore = context.applicationContext.themeDataStore

    override suspend fun load(): ThemeDto? = try {
        dataStore.data.first()[KEY_JSON]?.let { AppJson.decodeFromString(ThemeDto.serializer(), it) }
    } catch (e: IOException) {
        null
    } catch (e: SerializationException) {
        null
    }

    override suspend fun save(theme: ThemeDto) {
        val json = AppJson.encodeToString(ThemeDto.serializer(), theme)
        dataStore.edit { it[KEY_JSON] = json }
    }

    private companion object {
        val KEY_JSON = stringPreferencesKey("theme_json")
    }
}
