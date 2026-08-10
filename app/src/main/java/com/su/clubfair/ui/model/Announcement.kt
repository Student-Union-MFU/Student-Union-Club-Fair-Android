package com.su.clubfair.ui.model

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
 * [timestamp] is a pre-formatted string rather than an instant. Nothing here is
 * live yet, and a real feed would carry the clock and the locale rules with it;
 * inventing a formatter for six hard-coded posts would be inventing the wrong
 * half of that.
 */
data class Announcement(
    val id: Int,
    val author: String,
    val timestamp: String,
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

/** Stand-in feed until there is somewhere to post from. */
val PlaceholderAnnouncements = listOf(
    Announcement(
        id = 1,
        author = "Student Union",
        timestamp = "Yesterday at 18:04",
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
        timestamp = "Today at 09:12",
        body = "We're open. All 27 booths are live. Zone maps are on the boards by " +
            "the entrance if you'd rather wander than plan.",
        reactions = listOf(
            Reaction("👍", 63, mine = true),
            Reaction("❤️", 21),
        ),
    ),
    Announcement(
        id = 3,
        author = "Student Union",
        timestamp = "Today at 12:30",
        body = "Muay Thai club is running a demo at the main stage in 15 minutes.",
        reactions = listOf(Reaction("👀", 9)),
    ),
    Announcement(
        id = 4,
        author = "Student Union",
        timestamp = "Today at 14:45",
        body = "Reminder: 20 stamps gets you into the prize draw, and the draw " +
            "closes at 17:00 sharp tomorrow. No late entries, sorry.",
        reactions = listOf(
            Reaction("👍", 28),
            Reaction("😂", 4),
        ),
    ),
)
