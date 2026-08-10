package com.su.clubfair.data

/**
 * Where the policy links go.
 *
 * **These pages have to exist before release.** Sign-up makes a student agree to
 * both by tapping Create Account, and until now neither the sign-up notice nor
 * the pair of labels on the profile was a link at all — they were styled text
 * that consented to documents nobody could read. Google Play also requires a
 * reachable privacy policy URL on the store listing, so this is a shipping
 * blocker rather than a nicety.
 *
 * Kept as constants rather than string resources deliberately: a URL is not
 * translatable copy, and a localised policy belongs behind one address that
 * serves the right language, not behind two addresses an app has to choose
 * between.
 */
object Links {
    const val Terms = "https://su.mfu.ac.th/clubfair/terms"
    const val Privacy = "https://su.mfu.ac.th/clubfair/privacy"
}
