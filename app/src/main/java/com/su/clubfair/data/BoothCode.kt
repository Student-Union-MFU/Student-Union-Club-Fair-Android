package com.su.clubfair.data

/**
 * Reads a booth number out of a scanned QR payload.
 *
 * This replaces `Regex("""(\d{1,2})\s*$""")`, which took the trailing digits of
 * *anything*. That matched a phone number on a poster, a room sign, a URL to an
 * unrelated page, and a friend's contact card — all of which resolved to a booth
 * and ticked a checkpoint. The failure was silent and in the student's favour,
 * which is the worst direction for it to fail in when there is a prize draw at
 * the end.
 *
 * The accepted forms are now closed, and all of them name the fair explicitly
 * except the bare number:
 *
 *  - `clubfair://booth/7`      — the app's own scheme
 *  - `https://su.mfu.ac.th/fair/booth/07`
 *  - `CF-B07`                  — what fits under a printed code
 *  - `7` / `07`                — a bare number, and nothing else on the line
 *
 * Case-insensitive, tolerant of surrounding whitespace and a trailing slash, and
 * nothing else. A payload that merely *contains* a number is rejected.
 *
 * **This is still not a checkpoint.** Every form above is guessable from a
 * booth's number, so a student can mint a valid code for a booth they never
 * visited, and two students can scan each other's screens. Only a server can fix
 * that — by signing each booth's code and recording who redeemed it. What this
 * function buys is the accident case: it stops the app crediting a booth because
 * someone pointed the camera at a price tag.
 */
object BoothCode {

    private val Patterns = listOf(
        Regex("""^clubfair://booth/(\d{1,2})/?$""", RegexOption.IGNORE_CASE),
        Regex("""^https?://[^\s/]+/fair/booth/(\d{1,2})/?$""", RegexOption.IGNORE_CASE),
        Regex("""^cf-b(\d{1,2})$""", RegexOption.IGNORE_CASE),
        Regex("""^(\d{1,2})$"""),
    )

    /**
     * The booth number in [payload], or null if it is not a booth code.
     *
     * [boothCount] bounds the result: `Booth 61` is not a booth at a fair with
     * 27 of them, and accepting it would put an empty panel on screen instead of
     * saying the code is wrong.
     */
    fun parse(payload: String, boothCount: Int): Int? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null

        val number = Patterns.firstNotNullOfOrNull { pattern ->
            pattern.matchEntire(trimmed)?.groupValues?.get(1)?.toIntOrNull()
        } ?: return null

        return number.takeIf { it in 1..boothCount }
    }

    /** The canonical form to print under a booth's QR code. */
    fun format(booth: Int): String = "clubfair://booth/$booth"
}
