package com.su.clubfair.data.net

import com.su.clubfair.BuildConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The one HTTP client for the process.
 *
 * One instance, deliberately: OkHttp keeps its connection pool and thread pool on
 * the client, so building one per request throws away connection reuse and leaks
 * threads. It is documented as intended to be shared.
 *
 * The timeouts are shorter than OkHttp's ten-second defaults on purpose. A
 * student is standing in a hall holding a phone up at a booth; a request that has
 * not answered in five seconds is not going to answer usefully, and failing fast
 * is what lets the scan queue locally and the UI say so. The read timeout is the
 * longer of the two because it covers bcrypt on the server — sign-in genuinely
 * takes a moment.
 */
object Http {

    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        // OkHttp retries an idempotent request against another route on failure.
        // Left on: check-ins carry a client_id precisely so a retry is safe, and
        // every other call is a GET.
        .retryOnConnectionFailure(true)
        // Supplied per build variant from src/debug and src/release rather than by
        // a `BuildConfig.DEBUG` branch: the release build has no such file, so it
        // carries no logging code and no chance of a bearer token reaching logcat.
        .apply { httpLoggingInterceptor()?.let { addInterceptor(it) } }
        .build()

    /**
     * Where the server is, from `BuildConfig`.
     *
     * Debug points at 10.0.2.2 — the emulator's alias for the host — so a debug
     * build talks to `make dev` with no configuration. Release has no default and
     * must be supplied with `-PclubfairApiBase`; see the app's build file.
     */
    val baseUrl: String get() = BuildConfig.API_BASE_URL

    /**
     * Whether the app has somewhere to talk to at all.
     *
     * False only in a release build assembled without `-PclubfairApiBase`. Checked
     * rather than assumed, because the alternative is every request failing as
     * "offline" and a student being told to find better signal.
     */
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}
