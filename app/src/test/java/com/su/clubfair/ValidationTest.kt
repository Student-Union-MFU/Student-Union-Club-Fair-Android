package com.su.clubfair

import com.su.clubfair.data.EmailAddress
import com.su.clubfair.data.PasswordPolicy
import com.su.clubfair.data.PasswordProblem
import com.su.clubfair.data.PhoneNumber
import com.su.clubfair.data.StudentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules the sign-in and sign-up forms enforce.
 *
 * Both forms previously accepted anything, including nothing, so none of this
 * behaviour existed to be tested.
 */
class PhoneNumberTest {

    @Test
    fun `accepts a national mobile number`() {
        assertTrue(PhoneNumber.isValid("0683150329"))
        assertTrue(PhoneNumber.isValid("0812345678"))
        assertTrue(PhoneNumber.isValid("0912345678"))
    }

    @Test
    fun `accepts the same number however it is punctuated`() {
        listOf(
            "068 315 0329",
            "068-315-0329",
            "(068) 315 0329",
            "+66 68 315 0329",
            "+66683150329",
        ).forEach { typed ->
            assertEquals("normalising $typed", "0683150329", PhoneNumber.normalise(typed))
            assertTrue("should be valid: $typed", PhoneNumber.isValid(typed))
        }
    }

    @Test
    fun `matches a stored number against any typed form of it`() {
        assertTrue(PhoneNumber.matches("0683150329", "+66 68 315 0329"))
        assertTrue(PhoneNumber.matches("068-315-0329", "0683150329"))
        assertFalse(PhoneNumber.matches("0683150329", "0683150328"))
    }

    @Test
    fun `an empty stored number matches nothing`() {
        // Otherwise a device with no phone on file would let any number sign in.
        assertFalse(PhoneNumber.matches("", ""))
        assertFalse(PhoneNumber.matches("", "0683150329"))
    }

    @Test
    fun `rejects landlines, short numbers and long numbers`() {
        assertFalse(PhoneNumber.isValid("053916000"))
        assertFalse(PhoneNumber.isValid("068315032"))
        assertFalse(PhoneNumber.isValid("06831503299"))
        assertFalse(PhoneNumber.isValid("0783150329"))
        assertFalse(PhoneNumber.isValid(""))
    }
}

class PasswordPolicyTest {

    @Test
    fun `accepts eight characters with a letter and a digit`() {
        assertTrue(PasswordPolicy.isValid("clubfair1"))
        assertTrue(PasswordPolicy.isValid("a1234567"))
    }

    @Test
    fun `names the specific rule that was broken`() {
        // A field that only says "invalid" makes someone guess which of three
        // rules they missed.
        assertEquals(PasswordProblem.TooShort, PasswordPolicy.check("ab1"))
        assertEquals(PasswordProblem.NeedsLetter, PasswordPolicy.check("12345678"))
        assertEquals(PasswordProblem.NeedsDigit, PasswordPolicy.check("clubfairs"))
        assertEquals(PasswordProblem.Ok, PasswordPolicy.check("clubfair1"))
    }

    @Test
    fun `an empty password is too short rather than acceptable`() {
        assertEquals(PasswordProblem.TooShort, PasswordPolicy.check(""))
    }
}

class StudentIdTest {

    @Test
    fun `accepts ten digits`() {
        assertTrue(StudentId.isValid("6831503029"))
    }

    @Test
    fun `accepts a spaced id and stores it packed`() {
        assertTrue(StudentId.isValid("683 150 3029"))
        assertEquals("6831503029", StudentId.normalise("683 150 3029"))
    }

    @Test
    fun `rejects anything that is not ten digits`() {
        assertFalse(StudentId.isValid("683150302"))
        assertFalse(StudentId.isValid("68315030299"))
        assertFalse(StudentId.isValid(""))
        assertFalse(StudentId.isValid("abcdefghij"))
    }
}

class EmailAddressTest {

    @Test
    fun `accepts an address that could receive mail`() {
        assertTrue(EmailAddress.isValid("yion.sur@lamduan.mfu.ac.th"))
        assertTrue(EmailAddress.isValid(" yion@example.com "))
    }

    @Test
    fun `rejects the typos worth catching on a form`() {
        assertFalse(EmailAddress.isValid("yion.sur"))
        assertFalse(EmailAddress.isValid("yion@lamduan"))
        assertFalse(EmailAddress.isValid("yion @example.com"))
        assertFalse(EmailAddress.isValid(""))
    }
}
