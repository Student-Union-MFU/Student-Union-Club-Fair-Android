package com.su.clubfair.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.booleanResource
import com.su.clubfair.R

/**
 * One thing that happens at the fair, at a time.
 *
 * The times are instants rather than formatted strings, for the reason
 * [Announcement] carries one: a baked "09:00" cannot be translated, cannot
 * follow a 24-hour locale, and is wrong the moment the Student Union moves the
 * entry. Formatting happens on screen, in the reader's own locale — see
 * `ProgramScreen`.
 *
 * [endsAtMillis] is genuinely optional and is not a gap in the data. Half a
 * running order is points in time — an opening, a prize draw — and giving those
 * an invented end so the type is simpler would put a duration on screen that
 * nobody scheduled. What that means for "is this on now" is [ProgramStep]'s
 * problem rather than this type's.
 */
data class ProgramEntry(
    val id: Int,
    val startsAtMillis: Long,
    val endsAtMillis: Long?,
    /** Thai, the original. */
    val title: String,
    /** English, or null to fall back to [title]. */
    val titleEn: String?,
    val detail: String?,
    val detailEn: String?,
    val location: String?,
    val locationEn: String?,
    /** The zone letter, for entries that happen on the floor. Null for a stage. */
    val zoneCode: String?,
)

/**
 * Where an entry stands against the clock.
 *
 * Three states because the diagram draws three: a step already walked, the one
 * the student is standing on, and the ones ahead.
 */
enum class ProgramStatus { Done, Running, Upcoming }

/** An entry and where it stands — what the route on `ProgramScreen` is built from. */
data class ProgramStep(
    val entry: ProgramEntry,
    val status: ProgramStatus,
)

/**
 * The running order, resolved against a clock.
 *
 * The interesting part is what counts as the end of an entry that has no
 * `ends_at`. It runs **until the next one starts**, which is how a printed
 * running order is read and what makes exactly one row the current one at any
 * moment. Inventing a fixed duration instead would either overlap the next entry
 * or leave a gap where the screen says nothing is happening while the hall is
 * plainly busy.
 *
 * The last entry is the case that has no next, and [fairEndMillis] is what bounds
 * it — the fair's own closing time, which the app already knows. Without it a
 * final entry with no end would still be "happening now" at three in the morning.
 *
 * Ordering is the server's (`starts_at, id`) and is not re-sorted here. If the
 * server's order and this function's assumptions ever disagree, the server is
 * right and this is the thing to fix.
 */
fun List<ProgramEntry>.stepsAt(nowMillis: Long, fairEndMillis: Long): List<ProgramStep> =
    mapIndexed { index, entry ->
        val nextStart = getOrNull(index + 1)?.startsAtMillis
        val effectiveEnd = entry.endsAtMillis ?: nextStart ?: fairEndMillis
        ProgramStep(
            entry = entry,
            status = when {
                nowMillis < entry.startsAtMillis -> ProgramStatus.Upcoming
                nowMillis >= effectiveEnd -> ProgramStatus.Done
                else -> ProgramStatus.Running
            },
        )
    }

/** What is on right now, or null between entries and outside the fair. */
fun List<ProgramStep>.running(): ProgramStep? =
    firstOrNull { it.status == ProgramStatus.Running }

/** The next thing due to start, or null once the last one has begun. */
fun List<ProgramStep>.upNext(): ProgramStep? =
    firstOrNull { it.status == ProgramStatus.Upcoming }

/**
 * Which of the server's two languages to read, on the same terms as
 * [Booth.displayName] — see `BoothCopy` for why this is a resource and not a
 * `Locale` comparison.
 */
@Composable
private fun preferThai(): Boolean = booleanResource(R.bool.prefer_thai_names)

/** The entry's title in the reader's language, falling back to the one that exists. */
@Composable
fun ProgramEntry.displayTitle(): String =
    if (preferThai()) title else titleEn ?: title

/** Its blurb, or null when the Student Union has not written one. */
@Composable
fun ProgramEntry.displayDetail(): String? =
    (if (preferThai()) detail else detailEn ?: detail)?.takeIf { it.isNotBlank() }

/** Where it happens, as free text. The zone letter is carried separately. */
@Composable
fun ProgramEntry.displayLocation(): String? =
    (if (preferThai()) location else locationEn ?: location)?.takeIf { it.isNotBlank() }

/**
 * A stand-in running order for `@Preview`. Nothing in the running app reaches
 * for it — `clubfair_program` is empty today, so this is also the only way to
 * see the screen with anything on it.
 *
 * Offsets from a caller-supplied `now` rather than absolute dates, so the
 * preview shows one entry finished, one running and two ahead whenever it is
 * rendered.
 */
fun previewProgram(nowMillis: Long): List<ProgramEntry> {
    val hour = 60 * 60 * 1000L
    return listOf(
        ProgramEntry(
            id = 1,
            startsAtMillis = nowMillis - 3 * hour,
            endsAtMillis = nowMillis - 2 * hour,
            title = "พิธีเปิดงาน Club Fair",
            titleEn = "Opening ceremony",
            detail = "กล่าวเปิดงานโดยองค์การนักศึกษา",
            detailEn = "Opened by the Student Union.",
            location = "เวทีกลาง",
            locationEn = "Main stage",
            zoneCode = null,
        ),
        ProgramEntry(
            id = 2,
            startsAtMillis = nowMillis - 1 * hour,
            endsAtMillis = nowMillis + 1 * hour,
            title = "เดินชมบูธชมรม",
            titleEn = "Walk the club booths",
            detail = "สแกน QR ที่แต่ละบูธเพื่อสะสมแต้ม",
            detailEn = "Scan the QR at each booth to collect checkpoints.",
            location = null,
            locationEn = null,
            zoneCode = "A",
        ),
        ProgramEntry(
            id = 3,
            startsAtMillis = nowMillis + 2 * hour,
            endsAtMillis = nowMillis + 3 * hour,
            title = "การแสดงของชมรมนาฏศิลป์",
            titleEn = "Dance club showcase",
            detail = null,
            detailEn = null,
            location = "เวทีกลาง",
            locationEn = "Main stage",
            zoneCode = null,
        ),
        ProgramEntry(
            id = 4,
            startsAtMillis = nowMillis + 5 * hour,
            endsAtMillis = null,
            title = "จับรางวัล MFU333",
            titleEn = "MFU333 prize draw",
            detail = null,
            detailEn = null,
            location = "จุดองค์การนักศึกษา",
            locationEn = "Student Union desk",
            zoneCode = null,
        ),
    )
}
