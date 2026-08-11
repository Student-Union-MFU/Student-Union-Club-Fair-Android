package com.su.clubfair.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import com.su.clubfair.R

/**
 * Which of the server's two names to show, and what to say about a booth.
 *
 * The booth roster is the one place the app renders text it did not write.
 * `booth` holds `name` in Thai and `name_en` in English for all 28 clubs, and
 * `clubfair_zone` does the same for the three areas — so the language setting
 * has to reach them too, or a student who picks English still reads a wall of
 * Thai club names with the English underneath in small print.
 *
 * The choice is made by [R.bool.prefer_thai_names] rather than by comparing
 * `Locale.getLanguage()`. Same answer, but the resource is resolved by the
 * qualifier matching that already picks the right `strings.xml`, so it tracks
 * the in-app override, "Follow phone", and any locale added later with no list
 * of language codes to maintain. See `values/bools.xml`.
 *
 * The name in the other language is **not** shown alongside it. It was, at
 * first, on the reasoning that the printed boards on the floor are in Thai
 * whatever the phone says — but the result was a screen a student had asked to
 * be in English with a line of Thai under every club and every zone, which is
 * not a bilingual screen, it is an untranslated one. Search still matches both,
 * so a student typing either name still finds the club; see `BoothSearch`.
 */
@Composable
private fun preferThai(): Boolean = booleanResource(R.bool.prefer_thai_names)

/** The booth's name in the reader's language, falling back to the one that exists. */
@Composable
fun Booth.displayName(): String =
    if (preferThai()) name else nameEn ?: name

/** The zone's name in the reader's language. */
@Composable
fun Zone.displayTitle(): String =
    if (preferThai()) title else titleEn ?: title

/**
 * What kind of club this is, in words — "Sports", "ชมรมด้านกีฬา".
 *
 * What now sits on the secondary line the other-language name used to hold. It
 * keeps the line — the tile sizes on the booth wall are built out of how many
 * lines a card carries — and replaces one piece of information the reader
 * already had, in a language they may not read, with one they did not.
 */
@Composable
fun Booth.categoryName(): String? = categoryLabel(category)

/**
 * What kinds of club stand in a zone — "Sports · Student relations".
 *
 * `clubfair_zone.intent` is the only column on the booths page the server does
 * not send in two languages, so a reader in English got one line of Thai under
 * every zone heading. It is also, read closely, not new information: "ชมรมด้าน
 * กีฬาและชมรมด้านนักศึกษาสัมพันธ์" is the list of `category` values on the booths
 * standing in zone B, and the app already has every one of those.
 *
 * So this is derived rather than translated. That is the difference between
 * stating a fact the server sent — these booths are in this zone and their
 * categories are these — and inventing a description of a zone, which is what
 * hand-writing an English `intent` here would be.
 *
 * Falls back to the server's own line when the categories produce nothing: an
 * empty zone, or one whose booths are all in a category this app predates.
 * Thai in an English UI beats a heading with a blank line under it.
 */
@Composable
fun zoneIntent(zone: Zone, booths: List<Booth>): String? {
    val labels = booths.map { it.category }.distinct().mapNotNull { categoryLabel(it) }
    return if (labels.isEmpty()) zone.subtitle else labels.joinToString("  ·  ")
}

/** A `category` token as the app says it, or null for one it has not been taught. */
@Composable
private fun categoryLabel(category: String): String? = when (category) {
    "sports" -> stringResource(R.string.booth_category_sports)
    "student_relations" -> stringResource(R.string.booth_category_student_relations)
    "volunteer" -> stringResource(R.string.booth_category_volunteer)
    "religion_and_culture" -> stringResource(R.string.booth_category_religion_and_culture)
    "academic" -> stringResource(R.string.booth_category_academic)
    else -> null
}

/**
 * One line on what the club does.
 *
 * **The fallback is placeholder copy.** `booth.about` is null on all 28 rows —
 * the column exists and the Student Union has not written it — so until they
 * do, this stands in with a line about the *kind* of club, chosen by the
 * booth's own `category`.
 *
 * Written per category rather than per club, and that is the deliberate limit
 * of it: the app knows a booth is a sports club because the server says so, and
 * knows nothing else about it. Inventing a sentence about what the Kendo Club
 * specifically does would put a fabricated claim about a real student society on
 * the card someone uses to decide where to walk — which is the same reason
 * `members`, `meets` and `venue` were deleted rather than kept.
 *
 * So each line is true of every club in its category, says something a student
 * can act on, and is replaced the moment a real `about` lands: the server's copy
 * always wins, one booth at a time, with no release needed.
 */
@Composable
fun Booth.blurb(): String =
    about?.takeIf { it.isNotBlank() } ?: stringResource(placeholderBlurbFor(category))

private fun placeholderBlurbFor(category: String): Int = when (category) {
    "sports" -> R.string.booth_about_sports
    "student_relations" -> R.string.booth_about_student_relations
    "volunteer" -> R.string.booth_about_volunteer
    "religion_and_culture" -> R.string.booth_about_religion_and_culture
    "academic" -> R.string.booth_about_academic
    // A category the app has not been taught. Says the one thing that is true
    // of every stall at the fair rather than guessing which of the five above
    // it is closest to.
    else -> R.string.booth_about_generic
}
