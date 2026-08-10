package com.su.clubfair.ui.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.su.clubfair.R
import com.su.clubfair.ui.theme.ZoneAccent

/** One club's stall at the fair. [number] is what's printed on the booth itself. */
data class Booth(
    val number: Int,
    val name: String,
    /** One line on what the club actually does — the reason to walk over. */
    val about: String,
    /**
     * When it regularly meets, e.g. "Wed 17:00", and where on campus.
     *
     * Neither is on the booth panel. Both are facts about a club you have
     * already joined, and neither helps anyone decide whether to stop walking,
     * which is the only decision that panel supports. They stay on the model
     * because a club directory will want them; nothing renders them today.
     */
    val meets: String,
    val venue: String,
    val members: Int,
    val zone: Zone,
    @DrawableRes val icon: Int,
    val scanned: Boolean,
)

/**
 * The three areas of the fair floor, as habitats.
 *
 * [code] is what's on the signage, [letter] is the same thing at a glance for
 * the zone card's tile, [title] is the habitat, [subtitle] is what kind of clubs
 * actually stand there, and [accent] is the colour the area is drawn in.
 *
 * [letter] is stored rather than sliced off the end of [code], so a zone that
 * ever becomes "Zone A1" doesn't quietly start showing a "1".
 *
 * There was a fourth thing here: nine mascot emoji per zone, handed to each
 * booth by its position in the zone and rendered as a spinning 3D stall on the
 * booth panel. The panel shows the club now — see `BoothSheet` — so the
 * mascots, the model and the renderer behind them are gone.
 */
enum class Zone(
    val code: String,
    val letter: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
) {
    Rainforest("Zone A", "A", "Rainforest", "Sports & wellness", ZoneAccent.Rainforest),
    Savannah("Zone B", "B", "Savannah", "Arts & culture", ZoneAccent.Savannah),
    DeepOcean("Zone C", "C", "Deep Ocean", "Academic & community", ZoneAccent.DeepOcean),
}

private data class BoothSeed(
    val name: String,
    val about: String,
    val meets: String,
    val venue: String,
    val members: Int,
    val zone: Zone,
    @DrawableRes val icon: Int,
)

/**
 * The 27 clubs, in booth order.
 *
 * Names and blurbs are placeholders — swap them for the real roster when it
 * lands. The blurb is the point of the list: a name and a tick tell a student
 * nothing about whether a booth is worth queueing for.
 */
private val BoothSeeds = listOf(
    BoothSeed(
        "Football Club",
        "7-a-side matches and open training every week",
        "Wed 17:00",
        "Main Field",
        142,
        Zone.Rainforest,
        R.drawable.ic_club_football,
    ),
    BoothSeed(
        "Basketball Club",
        "Pick-up games, drills and inter-school tournaments",
        "Tue 18:00",
        "Indoor Court",
        96,
        Zone.Rainforest,
        R.drawable.ic_club_basketball,
    ),
    BoothSeed(
        "Badminton Club",
        "Doubles ladders and coaching for every level",
        "Mon 17:30",
        "Gym Hall B",
        88,
        Zone.Rainforest,
        R.drawable.ic_club_badminton,
    ),
    BoothSeed(
        "Muay Thai Club",
        "Pad work, conditioning and beginner-safe sparring",
        "Thu 18:30",
        "Martial Arts Room",
        54,
        Zone.Rainforest,
        R.drawable.ic_club_muaythai,
    ),
    BoothSeed(
        "E-Sports Club",
        "Valorant and RoV squads, plus casual LAN nights",
        "Fri 19:00",
        "IT Lab 3",
        176,
        Zone.Rainforest,
        R.drawable.ic_club_esports,
    ),
    BoothSeed(
        "Volleyball Club",
        "Indoor and beach sessions, mixed teams welcome",
        "Wed 16:30",
        "Indoor Court",
        73,
        Zone.Rainforest,
        R.drawable.ic_club_volleyball,
    ),
    BoothSeed(
        "Table Tennis Club",
        "Practice tables all day and a termly singles cup",
        "Daily 12:00",
        "Student Centre",
        61,
        Zone.Rainforest,
        R.drawable.ic_club_tabletennis,
    ),
    BoothSeed(
        "Fitness & Yoga Club",
        "Morning yoga, strength basics and mobility work",
        "Mon 07:00",
        "Fitness Studio",
        119,
        Zone.Rainforest,
        R.drawable.ic_club_fitness,
    ),
    BoothSeed(
        "Swimming Club",
        "Lane training, stroke clinics and open-water trips",
        "Tue 06:30",
        "University Pool",
        47,
        Zone.Rainforest,
        R.drawable.ic_club_swimming,
    ),
    BoothSeed(
        "Photography Club",
        "Photo walks, studio lighting and print critiques",
        "Sat 09:00",
        "Arts Building",
        134,
        Zone.Savannah,
        R.drawable.ic_club_photo,
    ),
    BoothSeed(
        "Music Club",
        "Band jams, open mics and instruments to borrow",
        "Thu 18:00",
        "Music Room",
        151,
        Zone.Savannah,
        R.drawable.ic_club_music,
    ),
    BoothSeed(
        "Dance Crew",
        "Hip-hop and K-pop choreo, showcase every term",
        "Fri 18:30",
        "Dance Studio",
        128,
        Zone.Savannah,
        R.drawable.ic_club_dance,
    ),
    BoothSeed(
        "Thai Traditional Arts",
        "Classical dance, wai khru rites and craft workshops",
        "Sat 13:00",
        "Culture Hall",
        42,
        Zone.Savannah,
        R.drawable.ic_club_thaiarts,
    ),
    BoothSeed(
        "Film & Media Club",
        "Short film production, edit nights and screenings",
        "Wed 19:00",
        "Media Lab",
        85,
        Zone.Savannah,
        R.drawable.ic_club_film,
    ),
    BoothSeed(
        "Drama Club",
        "Stage acting, improv games and the annual play",
        "Tue 18:30",
        "Auditorium",
        63,
        Zone.Savannah,
        R.drawable.ic_club_drama,
    ),
    BoothSeed(
        "Art & Illustration Club",
        "Life drawing, digital art jams and zine making",
        "Sun 14:00",
        "Art Studio",
        97,
        Zone.Savannah,
        R.drawable.ic_club_art,
    ),
    BoothSeed(
        "Cosplay Club",
        "Costume builds, prop making and convention meetups",
        "Sat 15:00",
        "Craft Room",
        58,
        Zone.Savannah,
        R.drawable.ic_club_cosplay,
    ),
    BoothSeed(
        "Literature Club",
        "Book circles, creative writing and poetry nights",
        "Thu 17:00",
        "Library Annex",
        39,
        Zone.Savannah,
        R.drawable.ic_club_literature,
    ),
    BoothSeed(
        "Robotics Club",
        "Line-follower builds and national competition teams",
        "Mon 18:00",
        "Engineering Lab",
        102,
        Zone.DeepOcean,
        R.drawable.ic_club_robotics,
    ),
    BoothSeed(
        "Coding Club",
        "Weekly hack nights, algorithms and side projects",
        "Wed 18:00",
        "IT Lab 1",
        188,
        Zone.DeepOcean,
        R.drawable.ic_club_coding,
    ),
    BoothSeed(
        "Debate Society",
        "Parliamentary debate drills and public speaking",
        "Tue 17:30",
        "Seminar Room 2",
        66,
        Zone.DeepOcean,
        R.drawable.ic_club_debate,
    ),
    BoothSeed(
        "Business Club",
        "Case competitions, pitch nights and mentor talks",
        "Thu 16:00",
        "Management Bldg",
        113,
        Zone.DeepOcean,
        R.drawable.ic_club_business,
    ),
    BoothSeed(
        "Volunteer Club",
        "Campus clean-ups and community teaching trips",
        "Sat 08:00",
        "Volunteer Office",
        149,
        Zone.DeepOcean,
        R.drawable.ic_club_volunteer,
    ),
    BoothSeed(
        "Environment Club",
        "Recycling drives, tree planting and eco audits",
        "Fri 16:30",
        "Science Court",
        77,
        Zone.DeepOcean,
        R.drawable.ic_club_environment,
    ),
    BoothSeed(
        "International Students Club",
        "Language exchange, culture nights and a buddy scheme",
        "Fri 17:00",
        "International House",
        124,
        Zone.DeepOcean,
        R.drawable.ic_club_international,
    ),
    BoothSeed(
        "Japanese Culture Club",
        "Nihongo practice, anime nights and calligraphy",
        "Wed 17:30",
        "Language Centre",
        91,
        Zone.DeepOcean,
        R.drawable.ic_club_japanese,
    ),
    BoothSeed(
        "Startup Club",
        "Idea sprints, founder talks and a termly demo day",
        "Mon 19:00",
        "Innovation Hub",
        108,
        Zone.DeepOcean,
        R.drawable.ic_club_startup,
    ),
)

/** How many booths the fair has. The one place that number is stated. */
val BoothCount: Int = BoothSeeds.size

/**
 * The roster, with [scanned] marking the booths this student has recorded.
 *
 * The flip the old comment predicted has happened: the booths are the source of
 * truth now and every count is derived from them. This used to take a `visited`
 * count and mark the first N booths scanned, which meant the ticks on the wall
 * were a rendering of a number rather than of anything the student had done —
 * scanning booth 23 first would have lit up booth 1.
 */
fun boothRoster(scanned: Set<Int> = emptySet()): List<Booth> =
    BoothSeeds.mapIndexed { index, seed ->
        val number = index + 1
        Booth(
            number = number,
            name = seed.name,
            about = seed.about,
            meets = seed.meets,
            venue = seed.venue,
            members = seed.members,
            zone = seed.zone,
            icon = seed.icon,
            scanned = number in scanned,
        )
    }

/**
 * A roster with the first [visited] booths ticked, for `@Preview` only.
 *
 * Previews have no store to read, and a wall of 27 empty tiles shows neither of
 * the two states the tiles exist to distinguish.
 */
fun previewRoster(visited: Int): List<Booth> = boothRoster((1..visited).toSet())
