package com.su.clubfair.ui

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import java.util.Locale

/**
 * A language the app can be read in.
 *
 * [System] is the default and is not the same as [English]: a phone set to Thai
 * opens the app in Thai with nobody choosing anything, and that is the behaviour
 * most students never have to think about. The other two exist for the ones who
 * do — a Thai student who keeps their phone in English, or an exchange student
 * on a Thai handset — and neither of those is a rare case at MFU.
 *
 * [tag] is what goes in DataStore, so it is BCP-47 and not an ordinal: a stored
 * enum position breaks the moment this list is reordered, and it would break
 * silently, on someone else's phone.
 */
enum class AppLanguage(val tag: String?) {
    System(null),
    English("en"),
    Thai("th"),
    ;

    companion object {
        /** The language a stored tag names, falling back to following the phone. */
        fun ofTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && it.tag == tag } ?: System
    }
}

/**
 * Reads [content] in the chosen language, whatever the phone is set to.
 *
 * Done in the composition rather than by recreating the Activity in a localised
 * base context, which is the older way and the reason so many apps blink to
 * black when you change their language. Overriding the locals below re-resolves
 * every `stringResource` under it on the next frame — the switch happens under
 * the student's finger, on the settings screen they are looking at, with the
 * scroll position intact.
 *
 * All three locals are provided together on purpose. `stringResource` has been
 * through more than one implementation — [LocalContext]'s resources, then
 * [LocalResources] — and [LocalConfiguration] is what a good deal of layout code
 * reads for the current locale. Providing one and not the others is how you get
 * a screen that is half translated.
 *
 * A null [language] tag resolves to the phone's own context, and is provided
 * anyway. That is not a redundant wrapper — it is the whole reason this works.
 * Skipping the provider with an early `return` puts [content] at a second call
 * site, which Compose reads as a different subtree: switching language then
 * disposes the entire app and rebuilds it, and every `remember` in it goes too.
 * It looks like a working language switch and behaves like a relaunch — the
 * settings sheet you changed it from closes, the tab resets to Home, and a
 * half-filled sign-up form is gone. One call site, always.
 */
@Composable
fun ProvideAppLanguage(language: String?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // Keyed on the configuration as well as the tag: a rotation or a font-scale
    // change produces a new one, and a context built from the stale copy would
    // pin the old screen size for everything underneath.
    val localised = remember(language, context, configuration) {
        if (language == null) {
            context
        } else {
            val localisedConfig = Configuration(configuration).apply {
                setLocales(LocaleList(Locale.forLanguageTag(language)))
            }
            context.createConfigurationContext(localisedConfig)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localised,
        LocalResources provides localised.resources,
        LocalConfiguration provides localised.resources.configuration,
        content = content,
    )
}
