package com.example.dynamictheme.data

import com.example.dynamictheme.theme.ThemeDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ThemeApi {
    /** GET /theme?name=ocean → ThemeDto JSON. */
    @GET("theme")
    suspend fun getTheme(@Query("name") name: String?): ThemeDto
}

/** Where themes come from. Abstracted so the repository can be unit-tested. */
interface ThemeSource {
    suspend fun fetch(name: String?): ThemeDto
}

class RetrofitThemeSource(private val api: ThemeApi) : ThemeSource {
    override suspend fun fetch(name: String?): ThemeDto = api.getTheme(name)
}
