package com.su.clubfair.data.net

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.su.clubfair.BuildConfig
import kotlinx.serialization.Serializable

/**
 * Getting a Google ID token for su-server to verify.
 *
 * Credential Manager, not the old `GoogleSignInClient` — that one is deprecated
 * and its replacement is this. The app never inspects the token: it hands the raw
 * JWT to `POST /clubfair/auth/google`, and the server checks the signature, the
 * audience, `email_verified` and the `@lamduan.mfu.ac.th` domain. A client-side
 * check would prove nothing, since a client can be modified.
 *
 * The credential also carries a plain profile next to the token, which is kept as
 * a [GoogleAccount] and used to fill in what the server has no copy of — the
 * account photo, mostly. That is a display concern and never an authorisation
 * one; the split is spelled out on [GoogleAccount].
 *
 * ## What has to be configured before this works
 *
 * 1. A **Web** OAuth client in the Google console, whose id goes in
 *    `GOOGLE_WEB_CLIENT_ID` (see the app's build file). It is the *web* id even on
 *    Android: it becomes the token's `aud`, which is what su-server already
 *    compares against its own `GOOGLE_CLIENT_ID`.
 * 2. An **Android** OAuth client for this package and signing certificate, with
 *    the debug and release SHA-1 fingerprints registered. Without it Google
 *    refuses to issue a token at all.
 *
 * With neither set, [isConfigured] is false and the UI keeps the Google button
 * disabled rather than opening a sheet that cannot succeed.
 */
/**
 * What Google said about the person behind the token.
 *
 * The credential carries a small profile alongside the JWT and the app used to
 * drop all of it. su-server verifies the same token and stores most of what is
 * here — the name and the `picture` claim both land in `clubfair_users` — so
 * this is a second copy of facts the app mostly already has, kept as a fallback
 * for the fields the server can be missing. The phone is the honest example: the
 * server has none for a Google account until the student types one. See
 * `Student.filledFrom`, which spells out what each field is actually for.
 *
 * It is **not** evidence of anything. Every field here was read out of a bundle
 * on the device, so it decides what is *displayed* and never what is *allowed* —
 * su-server verifies the token's signature, audience, `email_verified` and MFU
 * domain, and remains the only thing that says who this is. Keeping the two
 * apart is why [isFor] exists: the stored copy is only ever applied to the
 * account whose address it actually matches.
 *
 * Persisted with the session because it has to outlive the sheet — a refresh
 * hours later rewrites the cached profile, and without this the avatar would
 * vanish on the first one.
 */
@Serializable
data class GoogleAccount(
    /** The Google address, which for a student is their `@lamduan.mfu.ac.th` one. */
    val email: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val displayName: String? = null,
    /** A `googleusercontent.com` URL, or null for an account with no photo. */
    val photoUrl: String? = null,
    /**
     * Rarely present — Google only supplies it for an account that has a verified
     * number attached, so the sign-up form still asks and still validates.
     */
    val phone: String? = null,
) {
    /**
     * Given and family name, falling back to splitting [displayName].
     *
     * The split is on the first space and is deliberately crude, because it is
     * only ever reached when Google sent a display name but no structured pair —
     * and in that case one word to the left is a better guess than an empty
     * field. Anything the server already knows wins over both.
     */
    val firstName: String? get() = givenName.orNull() ?: displayName.orNull()?.substringBefore(' ')

    val surname: String? get() =
        familyName.orNull() ?: displayName.orNull()?.substringAfter(' ', missingDelimiterValue = "").orNull()

    /**
     * Whether this profile belongs to [address].
     *
     * Checked before any of it is used. Two students share a phone often enough
     * at a fair, and without this a Google profile left over from the last one
     * would put their photo on the next student's pass.
     */
    fun isFor(address: String): Boolean = email.trim().equals(address.trim(), ignoreCase = true)

    internal companion object {
        fun from(credential: GoogleIdTokenCredential) = GoogleAccount(
            // `id` is the email address on this credential type, not an opaque key.
            email = credential.id,
            givenName = credential.givenName,
            familyName = credential.familyName,
            displayName = credential.displayName,
            photoUrl = credential.profilePictureUri?.toString(),
            phone = credential.phoneNumber,
        )
    }
}

/** Blank is the same as absent for every field Google sends. */
private fun String?.orNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

object GoogleSignIn {

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** What came back from the credential sheet. */
    sealed interface Result {
        /**
         * [idToken] is for su-server; [account] is for the screens.
         *
         * Two fields rather than one because they are trusted differently, and
         * keeping them in one object is how that distinction gets forgotten.
         */
        data class Token(val idToken: String, val account: GoogleAccount) : Result

        /** The student dismissed the sheet. Not an error — say nothing. */
        data object Cancelled : Result

        /** No Google account on the device, or none that Google would offer. */
        data object NoAccount : Result

        data class Failed(val cause: Throwable) : Result

        /** The build has no client id, so this could never have worked. */
        data object NotConfigured : Result
    }

    /**
     * Opens the credential sheet and returns the ID token.
     *
     * `filterByAuthorizedAccounts = false` so a student who has never used this
     * app still sees their accounts. The alternative shows an empty sheet on first
     * run, which reads as the button being broken.
     *
     * [context] must be an Activity context — Credential Manager shows UI.
     */
    suspend fun requestIdToken(context: Context): Result {
        if (!isConfigured) return Result.NotConfigured

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            // Every account is @lamduan.mfu.ac.th, so asking Google to offer only
            // that domain saves a student picking a personal account and being
            // turned away by the server afterwards.
            .setHostedDomainFilter(MFU_DOMAIN)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.Token(credential.idToken, GoogleAccount.from(credential))
        } catch (cancelled: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (none: NoCredentialException) {
            Result.NoAccount
        } catch (failure: GetCredentialException) {
            Log.w("GoogleSignIn", "credential request failed", failure)
            Result.Failed(failure)
        } catch (unexpected: Exception) {
            // createFrom throws plain exceptions on a credential of another type,
            // which is a real possibility if the sheet ever offers a passkey.
            Log.w("GoogleSignIn", "unexpected credential", unexpected)
            Result.Failed(unexpected)
        }
    }

    private const val MFU_DOMAIN = "lamduan.mfu.ac.th"
}
