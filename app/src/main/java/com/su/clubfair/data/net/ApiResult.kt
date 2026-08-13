package com.su.clubfair.data.net

/**
 * What came back, as a value rather than an exception.
 *
 * Three cases, because the app has three genuinely different things to do about
 * them, and collapsing any two of them produces a screen that lies:
 *
 *  - [Success] — got it.
 *  - [Offline] — the request never reached the server. The student may well be
 *    standing in a concrete hall with no signal, so this is expected operating
 *    conditions rather than an error: show the cached data and queue the write.
 *  - [Failure] — the server answered and said no. It carries su-server's own Thai
 *    message, which is written for a student, and the status so the caller can
 *    tell "your token expired" from "that code is not ours".
 *
 * The distinction that matters most is Offline versus Failure. Treating a dead
 * network as a rejection is what makes an app tell someone their scan was invalid
 * when it simply has not been sent yet.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Offline(val cause: Throwable) : ApiResult<Nothing>

    data class Failure(
        val status: Int,
        /** su-server's `{"error": ...}` body, already in the reader's language. */
        val message: String?,
    ) : ApiResult<Nothing>
}

/** The value, or null for either kind of failure. */
fun <T> ApiResult<T>.valueOrNull(): T? = (this as? ApiResult.Success)?.value

/**
 * True when the server rejected the caller's token and a re-login is needed.
 *
 * Only ever the server's answer. A call made with no token at all never reaches
 * the network and comes back as [isMissingToken] instead — see `ClubFairApi`,
 * where the difference is the reason `FairRepository` can end a session on this
 * without ending one on every authenticated call made from the sign-in screen.
 */
val ApiResult<*>.isUnauthorized: Boolean
    get() = this is ApiResult.Failure && status == 401

/** True when the call needed a token and the device had none — nothing was sent. */
val ApiResult<*>.isMissingToken: Boolean
    get() = this is ApiResult.Failure && status == ClubFairApi.MissingToken

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Offline -> this
    is ApiResult.Failure -> this
}
