package com.su.clubfair

import com.su.clubfair.data.net.GoogleAccount
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.model.filledFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Google profile fills gaps in the server's account and nothing else.
 *
 * Worth testing rather than eyeballing because the rule is a direction, not a
 * value: get it backwards and a student's saved surname silently reverts to
 * whatever Google has on file at every sign-in, which nobody would report as a
 * bug and everybody would find infuriating. The other half is the shared-phone
 * case — one student's photo must not reach the next student's pass.
 */
class GoogleAccountTest {

    private val google = GoogleAccount(
        email = "6831503029@lamduan.mfu.ac.th",
        givenName = "Yion",
        familyName = "Suriya",
        displayName = "Yion Suriya",
        photoUrl = "https://lh3.googleusercontent.com/a/photo",
        phone = "0683150329",
    )

    /**
     * An account with every optional field empty.
     *
     * Not quite what su-server sends — it fills the name and the avatar from the
     * token itself — but the gaps are the whole subject here, and a fixture that
     * arrives pre-filled would test nothing.
     */
    private val fresh = Student(
        id = 1,
        firstName = "Yion",
        surname = "Suriya",
        email = "6831503029@lamduan.mfu.ac.th",
        studentId = null,
        phone = null,
        school = null,
        major = null,
        avatarUrl = null,
        isStaff = false,
        role = "student",
        hasPassword = false,
        profileComplete = false,
    )

    @Test
    fun `fills what the server left empty`() {
        val filled = fresh.filledFrom(google)

        assertEquals("https://lh3.googleusercontent.com/a/photo", filled.avatarUrl)
        // Derived from the address, the same way the server derives it.
        assertEquals("6831503029", filled.studentId)
        assertEquals("0683150329", filled.phone)
    }

    @Test
    fun `never overwrites what the server already knows`() {
        val saved = fresh.copy(
            firstName = "Ratchanon",
            surname = "Ketkaew",
            phone = "0800000000",
            avatarUrl = "https://su.mfu.ac.th/uploads/avatar.png",
            studentId = "6831503029",
        )

        val filled = saved.filledFrom(google)

        // Every one of these has a different value on the Google side. The
        // server's wins, because it is what the student actually saved.
        assertEquals("Ratchanon", filled.firstName)
        assertEquals("Ketkaew", filled.surname)
        assertEquals("0800000000", filled.phone)
        assertEquals("https://su.mfu.ac.th/uploads/avatar.png", filled.avatarUrl)
        assertEquals(saved, filled)
    }

    @Test
    fun `ignores a profile belonging to someone else`() {
        // The shared-phone case: a Google account left in the store from the
        // last student, applied to the one who has just signed in by password.
        val other = fresh.copy(email = "6831503030@lamduan.mfu.ac.th")

        val filled = other.filledFrom(google)

        assertNull(filled.avatarUrl)
        assertNull(filled.phone)
        assertNull(filled.studentId)
        assertEquals(other, filled)
    }

    @Test
    fun `matches an address whatever its case or padding`() {
        assertTrue(google.isFor("  6831503029@LAMDUAN.MFU.AC.TH "))
        assertFalse(google.isFor("6831503030@lamduan.mfu.ac.th"))
    }

    @Test
    fun `no google profile leaves the account untouched`() {
        assertEquals(fresh, fresh.filledFrom(null))
    }

    @Test
    fun `treats blank server fields as missing`() {
        // su-server sends "" rather than null for a name it has never been
        // given, so a plain null check would leave the greeting empty.
        val blank = fresh.copy(firstName = "", surname = "  ", avatarUrl = "")

        val filled = blank.filledFrom(google)

        assertEquals("Yion", filled.firstName)
        assertEquals("Suriya", filled.surname)
        assertEquals("https://lh3.googleusercontent.com/a/photo", filled.avatarUrl)
    }

    @Test
    fun `splits a display name when google sends no structured pair`() {
        val displayOnly = GoogleAccount(
            email = "6831503029@lamduan.mfu.ac.th",
            displayName = "Yion Suriya",
        )

        assertEquals("Yion", displayOnly.firstName)
        assertEquals("Suriya", displayOnly.surname)
    }

    @Test
    fun `a one-word display name yields no surname`() {
        // `substringAfter` returns the whole string when the delimiter is
        // absent, which would put the first name in both fields and render
        // initials of "YY". The missing-delimiter value guards it.
        val oneWord = GoogleAccount(
            email = "6831503029@lamduan.mfu.ac.th",
            displayName = "Yion",
        )

        assertEquals("Yion", oneWord.firstName)
        assertNull(oneWord.surname)
    }

    @Test
    fun `blank google fields are absent, not empty strings`() {
        val empty = GoogleAccount(
            email = "6831503029@lamduan.mfu.ac.th",
            givenName = "",
            familyName = "   ",
            phone = "",
        )

        assertNull(empty.firstName)
        assertNull(empty.surname)
        // A blank phone must not pre-fill the sign-up field with whitespace and
        // then fail its own validation.
        assertNull(fresh.filledFrom(empty).phone)
    }

    @Test
    fun `a non-mfu google address contributes no student id`() {
        // Cannot normally happen — the credential sheet is filtered by hosted
        // domain — but the derivation must not invent an id from a gmail local
        // part if it ever does.
        val personal = GoogleAccount(email = "someone@gmail.com", photoUrl = "https://x/y")
        val onPersonal = fresh.copy(email = "someone@gmail.com")

        val filled = onPersonal.filledFrom(personal)

        assertNull(filled.studentId)
        assertEquals("https://x/y", filled.avatarUrl)
    }
}
