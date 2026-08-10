package com.su.clubfair.ui.model

import androidx.compose.ui.graphics.Color
import com.su.clubfair.data.net.ZoneDto
import com.su.clubfair.ui.theme.ZoneAccent

/**
 * One of the three areas of the fair floor, as the Student Union signs them.
 *
 * This was an `enum` of three hardcoded habitats, on the assumption they were a
 * design device with nothing behind them. They are not: Rainforest / Savannah /
 * Deep Ocean are the real zones on the floor plan, `clubfair_zone` holds them in
 * Thai and English, and the server is the source. Hence a data class built from
 * [ZoneDto] rather than a fixed set — a fourth zone next year is data, not a
 * release.
 *
 * The property names are the enum's, so the screens that render a zone did not
 * have to change.
 */
data class Zone(
    /** The letter on the signage, and the prefix of every booth code in the area. */
    val code: String,
    /** Thai — the original, as displayed on the boards. */
    val title: String,
    /** English, or null where no translation exists yet. */
    val titleEn: String?,
    /** What kind of clubs stand here, in the SU's own words. */
    val subtitle: String?,
    val sortOrder: Int,
) {
    /** Same thing as [code]; kept because the zone tile reads better this way. */
    val letter: String get() = code

    /**
     * The colour this area is drawn in.
     *
     * Local, not from the server. The floor plan gives decoration guidance —
     * green/brown/cream, orange/brown/yellow, blue/indigo — and these are the
     * app's reading of it against a near-black backdrop that has to hold white
     * text. A hex string in a database column would be a theme the design system
     * could not reason about.
     */
    val accent: Color get() = ZoneAccent.forCode(code)

    companion object {
        fun from(dto: ZoneDto) = Zone(
            code = dto.code,
            title = dto.name,
            titleEn = dto.nameEn,
            subtitle = dto.intent,
            sortOrder = dto.sortOrder,
        )
    }
}

/**
 * The three zones for `@Preview`.
 *
 * Wording from the floor plan so a preview looks like the running app rather than
 * like placeholder text.
 */
val PreviewZones = listOf(
    Zone("A", "โซนป่าดิบชื้น", "Rainforest Zone", "ศาสนา วัฒนธรรม และบำเพ็ญประโยชน์", 1),
    Zone("B", "โซนทุ่งหญ้าสะวันนา", "Savannah Zone", "กีฬาและนักศึกษาสัมพันธ์", 2),
    Zone("C", "โซนมหาสมุทรลึก", "Deep Ocean Zone", "วิชาการ", 3),
)
