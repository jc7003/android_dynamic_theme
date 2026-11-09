package com.example.dynamictheme.data

import com.example.dynamictheme.theme.ColorsDto
import com.example.dynamictheme.theme.DefaultTheme
import com.example.dynamictheme.theme.PaletteDto
import com.example.dynamictheme.theme.ThemeDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ThemeRepositoryTest {

    private fun dto(id: String, primary: String = "#112233") =
        ThemeDto(id = id, version = 1, colors = ColorsDto(light = PaletteDto(primary = primary)))

    private class FakeSource(var result: () -> ThemeDto) : ThemeSource {
        override suspend fun fetch(name: String?) = result()
    }

    private class FakeStore(var stored: ThemeDto? = null) : ThemeStore {
        override suspend fun load() = stored
        override suspend fun save(theme: ThemeDto) { stored = theme }
    }

    @Test
    fun `starts with default theme`() {
        val repo = ThemeRepository(FakeSource { error("unused") }, FakeStore())
        assertEquals(DefaultTheme.theme, repo.theme.value)
    }

    @Test
    fun `init emits cached theme`() = runTest {
        val repo = ThemeRepository(FakeSource { error("unused") }, FakeStore(dto("cached")))
        repo.init()
        assertEquals("cached", repo.theme.value.id)
    }

    @Test
    fun `init ignores invalid cached theme`() = runTest {
        val repo = ThemeRepository(FakeSource { error("unused") }, FakeStore(dto("bad", primary = "nope")))
        repo.init()
        assertEquals(DefaultTheme.ID, repo.theme.value.id)
    }

    @Test
    fun `successful refresh emits and caches`() = runTest {
        val store = FakeStore()
        val repo = ThemeRepository(FakeSource { dto("fresh") }, store)
        val result = repo.refresh("fresh")
        assertTrue(result.isSuccess)
        assertEquals("fresh", repo.theme.value.id)
        assertEquals("fresh", store.stored?.id)
    }

    @Test
    fun `theme is cached before it is emitted`() = runTest {
        // Regression: emitting first let a collector recreate() the Activity, cancelling the save.
        lateinit var repo: ThemeRepository
        var themeIdAtSave: String? = null
        val store = object : ThemeStore {
            override suspend fun load(): ThemeDto? = null
            override suspend fun save(theme: ThemeDto) { themeIdAtSave = repo.theme.value.id }
        }
        repo = ThemeRepository(FakeSource { dto("fresh") }, store)
        repo.refresh()
        assertEquals(DefaultTheme.ID, themeIdAtSave)
        assertEquals("fresh", repo.theme.value.id)
    }

    @Test
    fun `network failure keeps current theme`() = runTest {
        val store = FakeStore(dto("cached"))
        val repo = ThemeRepository(FakeSource { throw IOException("offline") }, store)
        repo.init()
        val result = repo.refresh()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("cached", repo.theme.value.id)
    }

    @Test
    fun `invalid api theme is rejected and not cached`() = runTest {
        val store = FakeStore()
        val repo = ThemeRepository(FakeSource { dto("broken", primary = "not-a-color") }, store)
        val result = repo.refresh("broken")
        assertTrue(result.exceptionOrNull() is InvalidThemeException)
        assertEquals(DefaultTheme.ID, repo.theme.value.id)
        assertNull(store.stored)
    }
}
