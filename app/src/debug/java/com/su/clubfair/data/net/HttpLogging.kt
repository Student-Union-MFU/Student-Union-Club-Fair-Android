package com.su.clubfair.data.net

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Request logging, debug builds only.
 *
 * A source-set split rather than a `BuildConfig.DEBUG` branch, and the difference
 * matters: this file does not exist in a release build, so neither does the
 * dependency or any chance of a bearer token reaching logcat on a student's phone.
 * A runtime `if` would ship both.
 *
 * `BASIC`, not `BODY`. Method, URL, status and timing is what diagnoses a
 * connection problem; the bodies here carry tokens, password fields and every
 * student's progress, and logcat is world-readable to anyone with adb.
 */
internal fun httpLoggingInterceptor(): Interceptor? =
    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
