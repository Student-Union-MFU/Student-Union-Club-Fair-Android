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

/**
 * Getting a Google ID token for su-server to verify.
 *
 * Credential Manager, not the old `GoogleSignInClient` — that one is deprecated
 * and its replacement is this. The app never inspects the token: it hands the raw
 * JWT to `POST /clubfair/auth/google`, and the server checks the signature, the
 * audience, `email_verified` and the `@lamduan.mfu.ac.th` domain. A client-side
 * check would prove nothing, since a client can be modified.
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
object GoogleSignIn {

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** What came back from the credential sheet. */
    sealed interface Result {
        data class Token(val idToken: String) : Result

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
            Result.Token(credential.idToken)
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
