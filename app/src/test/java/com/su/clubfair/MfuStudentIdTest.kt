package com.su.clubfair

import com.su.clubfair.data.MfuStudentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sign-up form fills the school in for the student, so the digit positions
 * it reads had better be the right ones.
 *
 * The case that matters most is the last one: an id whose school code is not in
 * the table must still leave the student a way to finish, because MFU can open a
 * school without telling us.
 */
class MfuStudentIdTest {

    @Test
    fun `reads the school out of a real id`() {
        // 68-3-15-03-029: entered 2568 BE, bachelor's, school 15, programme 03.
        assertEquals(
            "School of Applied Digital Technology",
            MfuStudentId.schoolOf("6831503029"),
        )
        assertEquals("1503", MfuStudentId.programmeCodeOf("6831503029"))
    }

    @Test
    fun `reads every school in the table`() {
        // One id per code, built around the two digits under test, so a typo in
        // the map is caught here rather than by a student who cannot find their
        // school in the list.
        assertEquals("School of Liberal Arts", MfuStudentId.schoolOf("6831001001"))
        assertEquals("School of Cosmetic Science", MfuStudentId.schoolOf("5631701022"))
        assertEquals("School of Medicine", MfuStudentId.schoolOf("6832101001"))
        assertEquals("School of Sinology", MfuStudentId.schoolOf("6832401001"))
    }

    @Test
    fun `narrows the majors to the school`() {
        val majors = MfuStudentId.majorsOf("6831503029")
        assertTrue(majors.contains("Software Engineering"))
        // A major from a different school must not be on the list.
        assertFalse(majors.contains("Cosmetic Science"))
    }

    @Test
    fun `offers no major of its own`() {
        // Deliberate: there is no current programme-code table, so the form asks
        // rather than guesses. This test is the tripwire — filling
        // MajorByProgrammeCode in should make it fail, at which point the screen
        // pre-selecting a major is the intended behaviour and this expectation
        // becomes the real major.
        assertNull(MfuStudentId.suggestedMajorOf("6831503029"))
    }

    @Test
    fun `an unknown school still leaves a way forward`() {
        // Code 99 is in no table. School unknown, but the dropdown falls back to
        // every major rather than to nothing.
        assertNull(MfuStudentId.schoolOf("6839901001"))
        assertTrue(MfuStudentId.majorsOf("6839901001").isNotEmpty())
    }

    /**
     * The intake rule, against whatever this build is configured for.
     *
     * Written to read `BuildConfig.INTAKE_PREFIXES` rather than to assert "69"
     * outright, because the point of the setting is that next year it is "70" —
     * and a test that has to be edited alongside a config change is a test that
     * gets edited without being thought about. What is actually asserted is the
     * shape: a configured prefix passes, everything else does not, and an empty
     * configuration admits everyone rather than no one.
     */
    @Test
    fun `only the configured intakes may sign up`() {
        val allowed = MfuStudentId.EligibleIntakes
        if (allowed.isEmpty()) {
            assertTrue(MfuStudentId.isEligibleIntake("6831503029"))
            return
        }

        val eligible = allowed.first() + "31503029"
        assertTrue(MfuStudentId.isEligibleIntake(eligible))

        // "00" is not a Buddhist-era year anyone has entered under.
        assertFalse(MfuStudentId.isEligibleIntake("0031503029"))
        // Not an id at all — the length rule still applies, so a bare prefix
        // cannot slip through by matching itself.
        assertFalse(MfuStudentId.isEligibleIntake(allowed.first()))
        assertFalse(MfuStudentId.isEligibleIntake(""))
    }

    @Test
    fun `rejects anything that is not ten digits`() {
        assertFalse(MfuStudentId.isValid("683150302"))
        assertFalse(MfuStudentId.isValid("68315030299"))
        assertFalse(MfuStudentId.isValid("6831503O29"))
        assertFalse(MfuStudentId.isValid(""))
        assertNull(MfuStudentId.schoolOf("not-an-id"))
    }
}
