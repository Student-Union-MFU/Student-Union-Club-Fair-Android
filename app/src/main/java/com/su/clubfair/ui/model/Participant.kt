package com.su.clubfair.ui.model

import com.su.clubfair.data.net.ParticipantDto

/**
 * Someone on the fair's roster, as an admin sees them.
 *
 * Deliberately not [Student]. That type is *the person holding this phone* —
 * half the app reads it to decide what to show and what the session may do — and
 * a stranger read off a scanned pass must never be able to arrive in the same
 * shape. Two types with overlapping fields is the cheapest guard there is
 * against the bug where the app quietly starts thinking it is signed in as
 * whoever was last scanned.
 *
 * [visited] is the server's own booth count, so a scanned pass answers "how far
 * along is this student" without a second request.
 */
data class Participant(
    val id: Int,
    val firstName: String,
    val surname: String,
    val email: String,
    val studentId: String?,
    val phone: String?,
    val school: String?,
    val major: String?,
    val role: AccountRole,
    val isFlagged: Boolean,
    val visited: Int,
) {
    val name: String get() = "$firstName $surname"

    /** The monogram the avatar draws, on the same rule [Student] uses. */
    val initials: String
        get() = "${firstName.take(1)}${surname.take(1)}".uppercase()

    companion object {
        fun from(dto: ParticipantDto) = Participant(
            id = dto.id,
            firstName = dto.firstName,
            surname = dto.surname,
            email = dto.email,
            studentId = dto.studentId,
            phone = dto.phone,
            school = dto.school,
            major = dto.major,
            role = accountRoleOf(dto.role),
            isFlagged = dto.isFlagged,
            visited = dto.visited,
        )
    }
}
