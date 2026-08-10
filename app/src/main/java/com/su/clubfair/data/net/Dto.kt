package com.su.clubfair.data.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shapes, exactly as su-server sends them.
 *
 * Deliberately separate from the `ui.model` types the screens render. They look
 * similar today and they are not the same thing: a DTO changes when the server
 * changes, a UI model changes when a screen changes, and collapsing them means
 * every field the API adds ripples into Compose — or worse, a screen quietly
 * depends on a nullable the server is free to stop sending.
 *
 * Every field the server marks nullable is nullable here. `about` and `icon` are
 * null on real rows today, and `rank` is null for a student who has scanned
 * nothing.
 */

@Serializable
data class SessionDto(
    val token: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Int,
    val role: String,
    @SerialName("first_name") val firstName: String,
    val surname: String,
    val email: String,
    val phone: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    val school: String? = null,
    val major: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("has_password") val hasPassword: Boolean = false,
)

@Serializable
data class BoothDto(
    val id: Int,
    val name: String,
    @SerialName("name_en") val nameEn: String? = null,
    val category: String,
    val zone: String? = null,
    @SerialName("booth_code") val boothCode: String? = null,
    val about: String? = null,
    val icon: String? = null,
    @SerialName("event_id") val eventId: Int? = null,
)

@Serializable
data class ZoneDto(
    val code: String,
    val name: String,
    @SerialName("name_en") val nameEn: String? = null,
    val intent: String? = null,
    @SerialName("sort_order") val sortOrder: Int,
)

@Serializable
data class ProgressDto(
    val visited: Int,
    val total: Int,
    @SerialName("visited_booth_ids") val visitedBoothIds: List<Int> = emptyList(),
    /** Null until the student has scanned something — rendered as an em dash. */
    val rank: Int? = null,
    val prizes: List<PrizeTierDto> = emptyList(),
)

@Serializable
data class PrizeTierDto(
    val id: Int,
    val threshold: Int,
    val name: String,
    val description: String? = null,
    val reached: Boolean = false,
    val claimed: Boolean = false,
)

@Serializable
data class CheckInDto(
    val id: Long,
    @SerialName("client_id") val clientId: String,
    @SerialName("booth_id") val boothId: Int,
    @SerialName("device_time") val deviceTime: String,
    @SerialName("server_received_at") val serverReceivedAt: String,
)

/**
 * The answer to a scan. `outcome` is one of `recorded`, `duplicate_request` or
 * `already_scanned` — all three arrive as 2xx, because "you already collected
 * this" is an answer rather than a failure.
 */
@Serializable
data class CheckInResultDto(
    val outcome: String,
    @SerialName("check_in") val checkIn: CheckInDto? = null,
)

@Serializable
data class CheckInRequest(
    /** The scanned QR verbatim. The server parses it; the client does not get to
     *  say which booth it was. */
    val payload: String,
    /** Idempotency key, minted before the request goes out so a retry after a
     *  timeout returns the original row instead of double-counting. */
    @SerialName("client_id") val clientId: String,
    @SerialName("device_time") val deviceTime: String,
)

@Serializable
data class AnnouncementDto(
    val id: Long,
    @SerialName("author_id") val authorId: Int? = null,
    val author: String,
    val body: String,
    @SerialName("posted_at") val postedAt: String,
    val reactions: List<ReactionDto> = emptyList(),
)

@Serializable
data class ReactionDto(
    val emoji: String,
    val count: Int,
    val mine: Boolean = false,
)

// ---- Requests ------------------------------------------------------------

@Serializable
data class GoogleSignInRequest(@SerialName("id_token") val idToken: String)

@Serializable
data class PasswordLoginRequest(val phone: String, val password: String)

@Serializable
data class RegisterRequest(
    @SerialName("first_name") val firstName: String,
    val surname: String,
    val email: String,
    val phone: String,
    @SerialName("student_id") val studentId: String? = null,
    val school: String? = null,
    val major: String? = null,
    val password: String,
)

@Serializable
data class UpdateProfileRequest(
    val phone: String? = null,
    val school: String? = null,
    val major: String? = null,
)

@Serializable
data class ReactionRequest(val emoji: String)

/** `{"error": "<Thai message>"}` — what su-server sends on every failure. */
@Serializable
data class ApiErrorDto(val error: String? = null)

/** `{"mine": true}` from the reaction toggle. */
@Serializable
data class ReactionStateDto(val mine: Boolean)
