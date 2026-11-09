package com.example.dynamictheme.data

import android.content.SharedPreferences
import android.content.res.AssetManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Fake backend: answers `GET /theme?name=<x>` with `assets/themes/<x>.json`.
 * Remove it from the OkHttp client (see ServiceLocator) to hit a real server.
 */
class MockThemeInterceptor(
    private val assets: AssetManager,
    private val prefs: SharedPreferences,
) : Interceptor {

    /** Persisted so you can test the cached-theme fallback across app restarts. */
    var simulateFailure: Boolean
        get() = prefs.getBoolean(KEY_FAIL, false)
        set(value) = prefs.edit().putBoolean(KEY_FAIL, value).apply()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(LATENCY_MS)

        if (simulateFailure) {
            return respond(request, 503, """{"error":"Service unavailable (simulated)"}""")
        }
        if (request.url.encodedPath != "/theme") {
            return respond(request, 404, """{"error":"Not found"}""")
        }
        val name = request.url.queryParameter("name") ?: DEFAULT_NAME
        if (!SAFE_NAME.matches(name)) {
            return respond(request, 400, """{"error":"Bad theme name"}""")
        }
        val body = try {
            assets.open("themes/$name.json").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            return respond(request, 404, """{"error":"Unknown theme '$name'"}""")
        }
        return respond(request, 200, body)
    }

    private fun respond(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody(JSON))
            .build()

    companion object {
        private const val KEY_FAIL = "simulate_failure"
        private const val LATENCY_MS = 400L
        private const val DEFAULT_NAME = "ocean"
        private val SAFE_NAME = Regex("^[a-z0-9_-]+$")
        private val JSON = "application/json".toMediaType()
    }
}
