package com.su.clubfair.data

/**
 * Form rules for the sign-in and sign-up screens.
 *
 * Plain functions over strings, deliberately knowing nothing about Compose, so
 * they are unit-testable without a device — the screens previously accepted
 * anything at all, including empty fields, so there was nothing to test.
 *
 * Each failure is its own value rather than a boolean, because a field that
 * says only "invalid" makes the person guess which of four rules they broke.
 */

/**
 * Thai mobile numbers, as typed and as stored.
 *
 * Stored in the national `0XXXXXXXXX` form. People type these with spaces, with
 * dashes, and with a `+66` prefix that drops the leading zero, and all three are
 * the same number — so sign-in normalises both sides before comparing rather
 * than telling someone their own number is unknown because they typed it with
 * spaces this time.
 */
object PhoneNumber {

    /** Thai mobile prefixes in use: 06, 08, 09. */
    private val NationalForm = Regex("""^0[689]\d{8}$""")

    /**
     * Digits only, with a `+66` international prefix folded back to a leading 0.
     *
     * Unambiguous by length: a national number is ten digits, so an eleven-digit
     * one opening `66` is `+66` with the zero dropped, which is how the number
     * is written everywhere outside Thailand.
     */
    fun normalise(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (digits.length == 11 && digits.startsWith("66")) {
            "0" + digits.drop(2)
        } else {
            digits
        }
    }

    fun isValid(raw: String): Boolean = NationalForm.matches(normalise(raw))

    /** Whether two typed forms name the same number. */
    fun matches(a: String, b: String): Boolean {
        val left = normalise(a)
        return left.isNotEmpty() && left == normalise(b)
    }
}

/** Why a password was rejected, or [Ok]. */
enum class PasswordProblem { Ok, TooShort, NeedsLetter, NeedsDigit }

object PasswordPolicy {

    /**
     * Eight characters, with at least one letter and one digit.
     *
     * Deliberately short of the maze of symbol classes that pushes people
     * towards `Password1!` and a sticky note. Length and two classes is the
     * floor worth enforcing on a form whose account holds a booth count.
     */
    const val MinLength = 8

    fun check(password: String): PasswordProblem = when {
        password.length < MinLength -> PasswordProblem.TooShort
        password.none(Char::isLetter) -> PasswordProblem.NeedsLetter
        password.none(Char::isDigit) -> PasswordProblem.NeedsDigit
        else -> PasswordProblem.Ok
    }

    fun isValid(password: String): Boolean = check(password) == PasswordProblem.Ok
}

/**
 * Mae Fah Luang student IDs: ten digits.
 *
 * Checked because this is what the pass QR encodes and what a booth reads off
 * it — a mistyped id produces a pass that scans as somebody else, or as nobody.
 */
object StudentId {
    private val Form = Regex("""^\d{10}$""")

    fun normalise(raw: String): String = raw.filter(Char::isDigit)

    fun isValid(raw: String): Boolean = Form.matches(normalise(raw))
}

object EmailAddress {
    // Deliberately loose. The only email that matters is one that can receive a
    // message, and no regex settles that — this rejects the typos worth
    // catching on the form and leaves the rest to a confirmation link.
    private val Form = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    fun isValid(raw: String): Boolean = Form.matches(raw.trim())
}

/** A person's name: anything non-blank. Nothing else is safe to assume. */
object PersonName {
    fun isValid(raw: String): Boolean = raw.trim().isNotEmpty()
}
