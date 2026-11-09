package com.example.dynamictheme

import android.app.Application
import android.content.Context
import com.example.dynamictheme.data.MockThemeInterceptor
import com.example.dynamictheme.data.RetrofitThemeSource
import com.example.dynamictheme.data.ThemeApi
import com.example.dynamictheme.data.ThemeCache
import com.example.dynamictheme.data.ThemeRepository
import com.example.dynamictheme.theme.AppJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Simple manual DI. */
object ServiceLocator {
    /** Swap for your real backend and drop the mock interceptor below. */
    private const val BASE_URL = "https://mock.theme.api/"

    lateinit var mockInterceptor: MockThemeInterceptor
        private set
    lateinit var themeRepository: ThemeRepository
        private set

    fun init(app: Application) {
        mockInterceptor = MockThemeInterceptor(
            app.assets,
            app.getSharedPreferences("mock_api", Context.MODE_PRIVATE),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor) // remove to use the real API
            .build()

        val api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ThemeApi::class.java)

        themeRepository = ThemeRepository(RetrofitThemeSource(api), ThemeCache(app))
    }
}
