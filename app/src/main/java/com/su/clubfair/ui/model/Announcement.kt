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
 * [postedAtMillis] is an instant, not a pre-formatted string. It used to be
 * `"Today at 12:30"` baked into the model, which is three bugs in one field: it
 * cannot be re-rendered in Thai, it cannot be re-rendered in a 24-hour locale,
 * and "Today" was a lie the moment the app was left open past midnight. The
 * channel formats it at draw time against the current clock — see
 * `EventsScreen`'s `rememberRelativeTime`. su-server sends RFC 3339 for the same
 * reason; `events.date`/`events.time` over there are VARCHAR and cannot be.
 *
 * The counts on each [Reaction] are everyone's, resolved by the server; `mine` is
 * resolved per caller from the requesting token.
 */
data class Announcement(
    val id: Long,
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

/** Four posts for `@Preview`, since the channel now comes from the server. */
val PreviewAnnouncements = listOf(
    Announcement(
        id = 1,
        author = "Student Union",
        postedAtMillis = System.currentTimeMillis() - 15 * 60 * 60 * 1000L,
        body = "เปิดงาน Club Fair พรุ่งนี้ 09:00 ที่ลานเฉลิมพระเกียรติ " +
            "พก QR ในโปรไฟล์มาด้วย ทุกบูธสแกนได้เลย",
        reactions = listOf(Reaction("🎉", 42), Reaction("👍", 17)),
    ),
    Announcement(
        id = 2,
        author = "Student Union",
        postedAtMillis = System.currentTimeMillis() - 3 * 60 * 60 * 1000L,
        body = "เปิดแล้ว! ครบทั้ง 28 บูธ 3 โซน — ป่าดิบชื้น ทุ่งหญ้าสะวันนา และมหาสมุทรลึก",
        reactions = listOf(Reaction("👍", 63, mine = true), Reaction("❤️", 21)),
    ),
    Announcement(
        id = 3,
        author = "Student Union",
        postedAtMillis = System.currentTimeMillis() - 40 * 60 * 1000L,
        body = "ชมรมมวยไทยโชว์สาธิตที่เวทีกลางอีก 15 นาที",
        reactions = listOf(Reaction("👀", 9)),
    ),
    Announcement(
        id = 4,
        author = "Student Union",
        postedAtMillis = System.currentTimeMillis() - 5 * 60 * 1000L,
        body = "ย้ำอีกครั้ง: เก็บครบ 20 แสตมป์ลุ้นรับรางวัล ปิดรับ 17:00 ตรง",
        reactions = listOf(Reaction("👍", 28), Reaction("😂", 4)),
    ),
)
