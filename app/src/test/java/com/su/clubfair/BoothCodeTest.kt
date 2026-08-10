package com.su.clubfair

import com.su.clubfair.data.BoothCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The parse that decides whether a scan counts.
 *
 * The `rejects` block is the reason this file exists. The previous
 * implementation was `Regex("""(\d{1,2})\s*$""")` — trailing digits of anything —
 * and every case below passed it, silently crediting a booth the student never
 * visited. Each is a payload a camera genuinely sweeps past in a hall full of
 * posters, price lists and other people's phones.
 */
class BoothCodeTest {

    private val boothCount = 27

    @Test
    fun `accepts the app's own scheme`() {
        assertEquals(7, BoothCode.parse("clubfair://booth/7", boothCount))
        assertEquals(7, BoothCode.parse("clubfair://booth/07", boothCount))
        assertEquals(27, BoothCode.parse("CLUBFAIR://BOOTH/27/", boothCount))
    }

    @Test
    fun `accepts a fair URL`() {
        assertEquals(7, BoothCode.parse("https://su.mfu.ac.th/fair/booth/07", boothCount))
        assertEquals(12, BoothCode.parse("http://su.mfu.ac.th/fair/booth/12/", boothCount))
    }

    @Test
    fun `accepts the printed short form`() {
        assertEquals(3, BoothCode.parse("CF-B03", boothCount))
        assertEquals(3, BoothCode.parse("cf-b3", boothCount))
    }

    @Test
    fun `accepts a bare number`() {
        assertEquals(1, BoothCode.parse("1", boothCount))
        assertEquals(9, BoothCode.parse("09", boothCount))
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertEquals(5, BoothCode.parse("  clubfair://booth/5  ", boothCount))
        assertEquals(5, BoothCode.parse("\n5\n", boothCount))
    }

    @Test
    fun `rejects payloads that merely end in digits`() {
        // Every one of these resolved to a booth under the old trailing-digit
        // regex. A phone number ends in two digits; so does a room sign.
        listOf(
            "0683150329",
            "Room B12",
            "https://example.com/some/other/page/7",
            "WIFI:S:ClubFair;T:WPA;P:letmein12;;",
            "Call us on 053 916 000",
            "booth-07",
        ).forEach { payload ->
            assertNull("should not be a booth code: $payload", BoothCode.parse(payload, boothCount))
        }
    }

    @Test
    fun `rejects a booth number the fair does not have`() {
        assertNull(BoothCode.parse("28", boothCount))
        assertNull(BoothCode.parse("0", boothCount))
        assertNull(BoothCode.parse("clubfair://booth/99", boothCount))
    }

    @Test
    fun `rejects empty and junk`() {
        assertNull(BoothCode.parse("", boothCount))
        assertNull(BoothCode.parse("   ", boothCount))
        assertNull(BoothCode.parse("clubfair://booth/", boothCount))
        assertNull(BoothCode.parse("clubfair://booth/abc", boothCount))
    }

    @Test
    fun `formats a code it can read back`() {
        (1..boothCount).forEach { booth ->
            assertEquals(booth, BoothCode.parse(BoothCode.format(booth), boothCount))
        }
    }
}
