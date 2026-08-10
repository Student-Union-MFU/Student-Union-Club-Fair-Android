package com.su.clubfair.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the device-local account.
 *
 * This exists because the alternative was worse, not because it is where
 * passwords belong. Sign-in has to check the password against *something*, and
 * until there is a server that something is on the phone. What must not happen
 * is the obvious version of that — writing the password into DataStore as
 * typed, where any backup extraction or rooted-device dump reads it straight
 * out, and where a student who reuses their university password everywhere
 * hands it over by installing this app.
 *
 * So: a random 128-bit salt per account, PBKDF2 over it, and only the salt and
 * the derived key are stored. A dump reveals neither the password nor anything
 * reusable against another service.
 *
 * `PBKDF2WithHmacSHA1` rather than the SHA-256 variant because that one arrived
 * in API 26 and this app's floor is 24. SHA-1 as a PBKDF2 PRF is not the broken
 * use of SHA-1 — collision resistance is not what HMAC leans on — and the
 * iteration count is what carries the cost here.
 *
 * **Replace this with server-side authentication.** Verifying a password on the
 * device that stores it proves only that this phone was told the right answer;
 * whoever owns the phone can edit the stored hash. It is a real local login, not
 * a security boundary.
 */
object PasswordHasher {

    private const val Algorithm = "PBKDF2WithHmacSHA1"
    private const val Iterations = 120_000
    private const val KeyLengthBits = 256
    private const val SaltLengthBytes = 16

    /** A fresh salt for a new account, Base64 for storage. */
    fun newSalt(): String {
        val salt = ByteArray(SaltLengthBytes)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /** Derives the stored form of [password] under [salt]. */
    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            Iterations,
            KeyLengthBits,
        )
        return try {
            val key = SecretKeyFactory.getInstance(Algorithm).generateSecret(spec).encoded
            Base64.encodeToString(key, Base64.NO_WRAP)
        } finally {
            // The spec holds a copy of the characters; wipe it rather than
            // leaving the password on the heap until GC gets round to it.
            spec.clearPassword()
        }
    }

    /**
     * Whether [password] derives to [expectedHash] under [salt].
     *
     * Constant-time comparison. A timing side channel on a local login is close
     * to theoretical, but the correct comparison is the same length of code as
     * the incorrect one.
     */
    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val actual = hash(password, salt)
        if (actual.length != expectedHash.length) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].code xor expectedHash[i].code)
        }
        return diff == 0
    }
}
