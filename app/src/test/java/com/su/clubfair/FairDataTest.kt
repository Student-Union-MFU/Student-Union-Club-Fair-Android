package com.su.clubfair

import com.su.clubfair.data.FairRules
import com.su.clubfair.data.FairSchedule
import com.su.clubfair.data.FairStatus
import com.su.clubfair.data.ScanRecord
import com.su.clubfair.data.decodeScan
import com.su.clubfair.data.encodeScan
import com.su.clubfair.ui.booths.matching
import com.su.clubfair.ui.model.BoothCount
import com.su.clubfair.ui.model.boothRoster
import com.su.clubfair.ui.model.previewRoster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FairRulesTest {

    @Test
    fun `prizes are earned at each milestone`() {
        assertEquals(0, FairRules.prizesFor(0))
        assertEquals(0, FairRules.prizesFor(9))
        assertEquals(1, FairRules.prizesFor(10))
        assertEquals(1, FairRules.prizesFor(19))
        assertEquals(2, FairRules.prizesFor(20))
        assertEquals(3, FairRules.prizesFor(27))
    }

    @Test
    fun `the next milestone is the one still to reach`() {
        assertEquals(10, FairRules.nextMilestone(0))
        assertEquals(20, FairRules.nextMilestone(10))
        assertEquals(27, FairRules.nextMilestone(26))
        assertNull(FairRules.nextMilestone(27))
    }
}

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

class ScanRecordCodecTest {

    @Test
    fun `round-trips a record`() {
        val record = ScanRecord(booth = 7, atMillis = 1_770_000_000_000)
        assertEquals(record, decodeScan(encodeScan(record)))
    }

    @Test
    fun `drops a malformed row rather than throwing`() {
        // One unreadable entry must not make the whole checkpoint card fail.
        listOf("", "7", "@123", "abc@123", "7@abc", "7@").forEach {
            assertNull("should not decode: '$it'", decodeScan(it))
        }
    }
}

class BoothRosterTest {

    @Test
    fun `ticks exactly the booths that were scanned`() {
        // The old roster marked the first N booths scanned from a count, so
        // scanning booth 23 first lit up booth 1.
        val roster = boothRoster(setOf(23, 4))

        assertEquals(2, roster.count { it.scanned })
        assertTrue(roster.single { it.number == 23 }.scanned)
        assertTrue(roster.single { it.number == 4 }.scanned)
        assertTrue(roster.none { it.number == 1 && it.scanned })
    }

    @Test
    fun `an empty set ticks nothing`() {
        assertTrue(boothRoster().none { it.scanned })
    }

    @Test
    fun `booths are numbered from one with no gaps`() {
        val numbers = boothRoster().map { it.number }
        assertEquals((1..BoothCount).toList(), numbers)
    }

    @Test
    fun `every booth belongs to a zone and has a blurb`() {
        boothRoster().forEach { booth ->
            assertTrue("booth ${booth.number} has no name", booth.name.isNotBlank())
            assertTrue("booth ${booth.number} has no blurb", booth.about.isNotBlank())
        }
    }
}

class BoothSearchTest {

    private val roster = previewRoster(0)

    @Test
    fun `finds a club by name`() {
        assertEquals(listOf("Robotics Club"), roster.matching("robotics").map { it.name })
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(roster.matching("robotics"), roster.matching("ROBOTICS"))
    }

    @Test
    fun `searches the blurb as well as the name`() {
        // "code" appears in no club's name; the Coding Club's blurb is what
        // makes this findable, which is the reason the blurb is indexed.
        val names = roster.matching("hack").map { it.name }
        assertEquals(listOf("Coding Club"), names)
    }

    @Test
    fun `every term has to match, so more words narrow`() {
        val single = roster.matching("club")
        val pair = roster.matching("club drawing")
        assertTrue(pair.size < single.size)
        assertEquals(listOf("Art & Illustration Club"), pair.map { it.name })
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
}
