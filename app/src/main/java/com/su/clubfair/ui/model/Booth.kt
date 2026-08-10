package com.su.clubfair.ui.model

import androidx.annotation.DrawableRes
import com.su.clubfair.R
import com.su.clubfair.data.net.BoothDto

/**
 * One club's stall at the fair, as the server has it.
 *
 * This replaced a 27-entry hardcoded roster of invented English clubs. The real
 * fair has 28, their names are Thai, and they come from `booth` — seeded from
 * club.pdf and placed by the floor plan in migration 000016.
 *
 * Three of the old fields are gone because no such data exists: `members`,
 * `meets` and `venue` were invented per club, and the Student Union has not
 * supplied any of them. [about] survives as a nullable, which is what it is: the
 * column exists and is empty on all 28 until someone writes the copy.
 */
data class Booth(
    /** The server's row id. What a check-in and the progress grid key on. */
    val id: Int,
    /** What is printed on the stall — "A1", "B16", "C5". Null before the floor is laid out. */
    val code: String?,
    /** Thai, the original. */
    val name: String,
    /** English, or null to fall back to [name]. */
    val nameEn: String?,
    /** `sports`, `student_relations`, … — what kind of club, not where it stands. */
    val category: String,
    /** The zone letter. Grouped by this; see [Zone]. */
    val zoneCode: String?,
    /** One line on what the club does. Null on every booth today. */
    val about: String?,
    /** Resolved from the server's neutral icon token; null where none fits. */
    @DrawableRes val icon: Int?,
    val scanned: Boolean,
) {
    /**
     * What to show as the booth's number when there is no printed code yet.
     *
     * The id is a poor substitute — it is document order from the seed, not the
     * sign — but it is a number the student can read back to staff, which nothing
     * else on the card is.
     */
    val displayCode: String get() = code ?: id.toString()
}

/**
 * The zone's colour, resolved from the booth's own zone letter.
 *
 * On the booth rather than looked up at each call site: a card, a search row and
 * the panel all tint by zone, and three lookups is three chances to fall back to
 * the house accent by accident.
 */
val Booth.accent: androidx.compose.ui.graphics.Color
    get() = com.su.clubfair.ui.theme.ZoneAccent.forCode(zoneCode.orEmpty())

/**
 * The server's icon token to a drawable.
 *
 * The mapping is here rather than in the database because the token is neutral by
 * design — 'football', not 'ic_club_football' — so each client resolves it to its
 * own asset. An unrecognised token behaves exactly like a missing one, which is
 * what lets the server add a token before the app has art for it.
 */
@DrawableRes
fun iconForToken(token: String?): Int? = when (token) {
    "environment" -> R.drawable.ic_club_environment
    "volunteer" -> R.drawable.ic_club_volunteer
    "school" -> R.drawable.ic_school
    "music" -> R.drawable.ic_club_music
    "thaiarts" -> R.drawable.ic_club_thaiarts
    "dance" -> R.drawable.ic_club_dance
    "members" -> R.drawable.ic_members
    "muaythai" -> R.drawable.ic_club_muaythai
    "football" -> R.drawable.ic_club_football
    "basketball" -> R.drawable.ic_club_basketball
    "volleyball" -> R.drawable.ic_club_volleyball
    "badminton" -> R.drawable.ic_club_badminton
    "swimming" -> R.drawable.ic_club_swimming
    "drama" -> R.drawable.ic_club_drama
    "debate" -> R.drawable.ic_club_debate
    "international" -> R.drawable.ic_club_international
    "photo" -> R.drawable.ic_club_photo
    // Everything else, including null: no glyph. Six booths are deliberately
    // without one — see migration 000017 — and the card shows the zone's letter
    // instead rather than a wrong picture.
    else -> null
}

fun boothFrom(dto: BoothDto, scanned: Boolean): Booth = Booth(
    id = dto.id,
    code = dto.boothCode,
    name = dto.name,
    nameEn = dto.nameEn,
    category = dto.category,
    zoneCode = dto.zone,
    about = dto.about,
    icon = iconForToken(dto.icon),
    scanned = scanned,
)

/**
 * A slice of the real roster for `@Preview`, with the first [visited] scanned.
 *
 * Real names and codes, so a preview shows what the fair actually looks like —
 * including the Thai typography, which is the thing most likely to be wrong and
 * least likely to be noticed against English placeholders.
 */
fun previewRoster(visited: Int = 0): List<Booth> {
    val seed = listOf(
        Triple("A1", "ชมรมอนุรักษ์พันธุ์สัตว์ป่าและป่าไม้", "environment"),
        Triple("A2", "ชมรมอาสาพัฒนาชุมชน", "volunteer"),
        Triple("A3", "ชมรมครูดอย", "school"),
        Triple("A4", "ชมรมพระพุทธศาสนา", null),
        Triple("A5", "ชมรมคริสเตียน", null),
        Triple("A6", "ชมรมดนตรีไทย-พื้นเมือง", "music"),
        Triple("A7", "ชมรมนาฏศิลป์", "thaiarts"),
        Triple("B1", "ชมรม DAC", null),
        Triple("B2", "ชมรมผู้นำเชียร์", "dance"),
        Triple("B3", "ชมรมแม่ฟ้าคฑากร", "music"),
        Triple("B4", "ชมรมสวัสดิการ", "members"),
        Triple("B5", "ชมรม BJJ", "muaythai"),
        Triple("B6", "ชมรมเคนโด้", "muaythai"),
        Triple("B7", "ชมรมมวยไทย", "muaythai"),
        Triple("B8", "ชมรมเทควันโด", "muaythai"),
        Triple("B9", "ชมรมหมากกระดาน", null),
        Triple("B10", "ชมรมฟุตบอล", "football"),
        Triple("B11", "ชมรมบาสเกตบอล", "basketball"),
        Triple("B12", "ชมรมรักบี้", "football"),
        Triple("B13", "ชมรมวอลเลย์บอล", "volleyball"),
        Triple("B14", "ชมรมแบดมินตัน", "badminton"),
        Triple("B15", "ชมรมเปตอง", null),
        Triple("B16", "ชมรมว่ายน้ำ", "swimming"),
        Triple("C1", "ชมรมการละคร", "drama"),
        Triple("C2", "ชมรมพิธีกร", "debate"),
        Triple("C3", "ชมรม MFUMUN", "international"),
        Triple("C4", "ชมรม MFU Human Rights Club", null),
        Triple("C5", "ชมรม MFU Photo Club", "photo"),
    )

    return seed.mapIndexed { index, (code, name, token) ->
        Booth(
            id = index + 1,
            code = code,
            name = name,
            nameEn = null,
            category = "sports",
            zoneCode = code.take(1),
            about = null,
            icon = iconForToken(token),
            scanned = index < visited,
        )
    }
}

/** How many booths the fair has, for previews only. The real count is the server's. */
const val PreviewBoothCount = 28
