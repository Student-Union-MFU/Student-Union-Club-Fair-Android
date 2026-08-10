package com.su.clubfair.ui.model

import com.su.clubfair.data.FairSchedule

/**
 * One emoji bucket under a post.
 *
 * [mine] is separate from [count] because the two answer different questions —
 * how many people reacted, and whether *you* are one of them — and a chip has to
 * show both at once: the number, and whether it is lit.
 */
data class Reaction(
    val emoji: String,
    val count: Int,
    val mine: Boolean = false,
)

/**
 * A post in the announcements channel.
 *
 * [postedAtMillis] is an instant, not a pre-formatted string. It used to be
 * `"Today at 12:30"` baked into the model, which is three bugs in one field: it
 * cannot be re-rendered in Thai, it cannot be re-rendered in a 24-hour locale,
 * and "Today" was a lie the moment the app was left open past midnight. The
 * channel formats it at draw time against the current clock — see
 * `EventsScreen`'s `rememberRelativeTime`.
 */
data class Announcement(
    val id: Int,
    val author: String,
    val postedAtMillis: Long,
    val body: String,
    val reactions: List<Reaction> = emptyList(),
)

/**
 * The five a student can pick from.
 *
 * A fixed set, not an emoji keyboard. The channel is one-way and the reactions
 * are the only thing a student can say back — five is enough to answer with
 * ("got it", "love it", "ha", "nice", "watching") and few enough that the picker
 * is one row that needs no scrolling, no search and no recently-used list.
 */
val ReactionPalette = listOf("👍", "❤️", "😂", "🎉", "👀")

private const val Hour = 60L * 60L * 1000L

/**
 * Stand-in feed until there is somewhere to post from.
 *
 * Timed relative to [FairSchedule] rather than to fixed dates, so moving the
 * fair to its real weekend carries the demo posts along with it instead of
 * leaving four announcements stranded in a July that has been and gone.
 *
 * The counts are invented. The *student's own* reactions are not — those are
 * read back out of `ClubFairStore` and merged over this list, which is why
 * `mine` is false on every seed here.
 */
val SeedAnnouncements = listOf(
    Announcement(
        id = 1,
        author = "Student Union",
        postedAtMillis = FairSchedule.startMillis - 15 * Hour,
        body = "Club Fair opens tomorrow at 09:00 on the C1 lawn. Bring your pass — " +
            "the QR is on your profile and every booth scans it.",
        reactions = listOf(
            Reaction("🎉", 42),
            Reaction("👍", 17),
        ),
    ),
    Announcement(
        id = 2,
        author = "Student Union",
        postedAtMillis = FairSchedule.startMillis + 12 * Hour / 60,
        body = "We're open. All 27 booths are live. Zone maps are on the boards by " +
            "the entrance if you'd rather wander than plan.",
        reactions = listOf(
            Reaction("👍", 63),
            Reaction("❤️", 21),
        ),
    ),
    Announcement(
        id = 3,
        author = "Student Union",
        postedAtMillis = FairSchedule.startMillis + 3 * Hour,
        body = "Muay Thai club is running a demo at the main stage in 15 minutes.",
        reactions = listOf(Reaction("👀", 9)),
    ),
    Announcement(
        id = 4,
        author = "Student Union",
        postedAtMillis = FairSchedule.startMillis + 6 * Hour,
        body = "Reminder: 20 stamps gets you into the prize draw, and the draw " +
            "closes at 17:00 sharp tomorrow. No late entries, sorry.",
        reactions = listOf(
            Reaction("👍", 28),
            Reaction("😂", 4),
        ),
    ),
)
