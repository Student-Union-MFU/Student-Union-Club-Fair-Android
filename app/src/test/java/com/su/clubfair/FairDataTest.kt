package com.su.clubfair

import com.su.clubfair.data.FairSchedule
import com.su.clubfair.data.FairStatus
import com.su.clubfair.ui.booths.matching
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.PreviewBoothCount
import com.su.clubfair.ui.model.iconForToken
import com.su.clubfair.ui.model.previewRoster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What is left to test on the client after su-server took over.
 *
 * Gone from here, and deliberately: the booth-code parse, the prize thresholds and
 * the roster's scanned state. All three are the server's now, and
 * `clubfair_checkin_service_test.go` covers the first two — a second copy in
 * Kotlin would be a copy that can disagree.
 *
 * The schedule stays because nothing serves it, and the search stays because it is
 * genuinely a client concern: it runs over an already-downloaded list.
 */
class FairScheduleTest {

    @Test
    fun `the fair opens before it closes`() {
        assertTrue(FairSchedule.startMillis < FairSchedule.endMillis)
    }

    @Test
    fun `reports the three phases around the window`() {
        val beforeStart = FairSchedule.statusAt(FairSchedule.startMillis - 60_000)
        assertTrue(beforeStart is FairStatus.BeforeStart)
        assertEquals(60_000L, (beforeStart as FairStatus.BeforeStart).untilStartMillis)

        val midway = FairSchedule.statusAt(
            (FairSchedule.startMillis + FairSchedule.endMillis) / 2,
        )
        assertTrue(midway is FairStatus.Running)

        assertEquals(FairStatus.Ended, FairSchedule.statusAt(FairSchedule.endMillis))
        assertEquals(FairStatus.Ended, FairSchedule.statusAt(FairSchedule.endMillis + 1))
    }
}

class BoothIconTokenTest {

    @Test
    fun `resolves the tokens the server seeds`() {
        listOf(
            "environment", "volunteer", "school", "music", "thaiarts", "dance",
            "members", "muaythai", "football", "basketball", "volleyball",
            "badminton", "swimming", "drama", "debate", "international", "photo",
        ).forEach { token ->
            assertNotNull("no drawable for token $token", iconForToken(token))
        }
    }

    @Test
    fun `an unknown token behaves exactly like a missing one`() {
        // This is what lets the server add a token before the app ships art for
        // it: both paths fall through to the booth-code fallback.
        assertNull(iconForToken(null))
        assertNull(iconForToken(""))
        assertNull(iconForToken("quidditch"))
    }
}

class BoothModelTest {

    @Test
    fun `the preview roster is the real fair's size and shape`() {
        val roster = previewRoster()
        assertEquals(PreviewBoothCount, roster.size)
        // 7 + 16 + 5, from the floor plan.
        assertEquals(7, roster.count { it.zoneCode == "A" })
        assertEquals(16, roster.count { it.zoneCode == "B" })
        assertEquals(5, roster.count { it.zoneCode == "C" })
    }

    @Test
    fun `booth codes run in signage order within a zone`() {
        val savannah = previewRoster().filter { it.zoneCode == "B" }.mapNotNull { it.code }
        assertEquals("B1", savannah.first())
        assertEquals("B16", savannah.last())
    }

    @Test
    fun `six booths have no icon, matching migration 000017`() {
        val without = previewRoster().filter { it.icon == null }.mapNotNull { it.code }
        assertEquals(listOf("A4", "A5", "B1", "B9", "B15", "C4").sorted(), without.sorted())
    }

    @Test
    fun `displayCode falls back to the id when the floor plan has not been set`() {
        val unplaced = Booth(
            id = 12,
            code = null,
            name = "ชมรมใหม่",
            nameEn = null,
            category = "academic",
            zoneCode = null,
            about = null,
            icon = null,
            scanned = false,
        )
        assertEquals("12", unplaced.displayCode)
    }
}

class BoothSearchTest {

    private val roster = previewRoster()

    @Test
    fun `finds a club by its Thai name`() {
        val names = roster.matching("ฟุตบอล").map { it.name }
        assertEquals(listOf("ชมรมฟุตบอล"), names)
    }

    @Test
    fun `finds a club by its booth code`() {
        // A student standing next to a sign knows the code and not much else.
        assertEquals(listOf("B7"), roster.matching("B7").mapNotNull { it.code })
    }

    @Test
    fun `finds a club by its English name when the server supplied one`() {
        val withEnglish = roster.map {
            if (it.code == "B10") it.copy(nameEn = "Football Club") else it
        }
        assertEquals(listOf("B10"), withEnglish.matching("football").mapNotNull { it.code })
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(roster.matching("mfu"), roster.matching("MFU"))
    }

    @Test
    fun `every term has to match, so more words narrow`() {
        val single = roster.matching("ชมรม")
        val pair = roster.matching("ชมรม MFU")
        assertTrue(pair.size < single.size)
    }

    @Test
    fun `an empty query matches nothing rather than everything`() {
        assertTrue(roster.matching("").isEmpty())
        assertTrue(roster.matching("   ").isEmpty())
    }

    @Test
    fun `no match returns empty`() {
        assertTrue(roster.matching("quidditch").isEmpty())
    }

    @Test
    fun `ordering is stable at equal rank`() {
        // Booth order, so the list does not reshuffle as a query is typed.
        val ids = roster.matching("ชมรม").map(Booth::id)
        assertEquals(ids.sorted(), ids)
    }
}
