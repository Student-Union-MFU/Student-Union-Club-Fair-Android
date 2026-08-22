package com.su.clubfair

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.su.clubfair.data.ScanOutcome
import com.su.clubfair.ui.ProvideAppLanguage
import com.su.clubfair.ui.booths.BoothsScreen
import com.su.clubfair.ui.components.GlassNavBar
import com.su.clubfair.ui.events.EventsScreen
import com.su.clubfair.ui.home.HomeScreen
import com.su.clubfair.ui.model.PreviewProgress
import com.su.clubfair.ui.model.PreviewStudent
import com.su.clubfair.ui.model.PreviewZones
import com.su.clubfair.ui.model.previewProgram
import com.su.clubfair.ui.model.previewRoster
import com.su.clubfair.ui.prizes.PrizesScreen
import com.su.clubfair.ui.profile.ProfileScreen
import com.su.clubfair.ui.program.ProgramScreen
import com.su.clubfair.ui.qr.QrTicketScreen
import com.su.clubfair.ui.scan.ScanScreen
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.SUClubFairTheme
import com.su.clubfair.ui.welcome.WelcomeScreen
import java.io.File
import java.io.FileOutputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the store screenshots, one PNG per test, into the app's external
 * files directory. `make screenshots` runs it and pulls the results.
 *
 * This is a generator wearing a test's clothes — it asserts nothing, because
 * what it produces is looked at rather than checked. It lives in `androidTest`
 * anyway for the one thing that buys: these are the real composables, rendered
 * by the real Compose runtime on a real device, so a screenshot cannot drift
 * from the app the way a mockup does. Regenerating the store listing's images
 * after a redesign is one command rather than an afternoon of cropping.
 *
 * **The student is a fixture, not a person.** [DemoStudent] exists because
 * `PreviewStudent` carries a name, an email and a student id that read as real
 * ones, and these images are published to the Play Store. Nothing here should
 * ever be pointed at a live account.
 *
 * The clock is frozen rather than left to run: the backdrop's blooms drift on an
 * infinite animation, and an infinite animation is exactly what makes
 * `waitForIdle` never return. [capture] stops the clock, advances it far enough
 * for the entry transitions to settle, and shoots that frame.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(
        name: String,
        language: String,
        /** The tab to light in the nav bar, or null for a screen the bar is not behind. */
        navTab: Int? = null,
        content: @Composable () -> Unit,
    ) {
        rule.mainClock.autoAdvance = false
        // `ScanScreen` remembers a permission launcher, and a launcher needs a
        // registry to register against. A bare Compose test host provides none —
        // the activity behind this rule does, so hand it over explicitly rather
        // than leaving the screen to find one that is not there.
        val activity = rule.activity
        rule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {
                SUClubFairTheme {
                    ProvideAppLanguage(language) {
                        // The arrangement from `AppShell`, and it has to be here
                        // rather than left to the screens: a tab does not paint
                        // the backdrop, the shell does. Rendering `HomeScreen` on
                        // its own is white text on the activity's white window —
                        // which is exactly how the first run of this came out.
                        val backdrop = rememberLayerBackdrop()
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(backdrop),
                            ) {
                                MeshBackground()
                                content()
                            }
                            if (navTab != null) {
                                GlassNavBar(
                                    selected = navTab,
                                    onSelect = {},
                                    backdrop = backdrop,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .safeDrawingPadding()
                                        .padding(
                                            horizontal = Dimens.ScreenPadding,
                                            vertical = Dimens.Space,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        // Long enough for a staggered entry to land, short enough that the
        // backdrop has not drifted anywhere interesting.
        rule.mainClock.advanceTimeBy(1_500)

        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        // Internal storage, not `getExternalFilesDir`: scoped storage keeps the
        // adb shell user out of `/sdcard/Android/data`, and the only way back in
        // is `run-as`, which works on this directory and not that one.
        val dir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "screenshots",
        ).apply { mkdirs() }
        // JPEG, not PNG, and the quality is the whole argument. These frames are
        // a dithered gradient behind translucent glass — the noise `MeshBackground`
        // adds on purpose to kill banding is also what makes PNG give up, at about
        // 2.9 MB a frame and 47 MB for a run. At quality 92 with no chroma
        // subsampling the same frame is 370 kB and measures 43.5 dB against the
        // lossless capture, which is past the point anyone can see. Play takes
        // JPEG for screenshots and re-encodes whatever it is given anyway.
        FileOutputStream(File(dir, "$language-$name.jpg")).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }
    }

    // ------------------------------------------------------------------ english

    @Test fun en_home() = capture("1-home", "en", navTab = 0) { Home() }
    @Test fun en_booths() = capture("2-booths", "en", navTab = 1) { Booths() }
    @Test fun en_scan() = capture("3-scan", "en") { Scan() }
    @Test fun en_prizes() = capture("4-prizes", "en") { Prizes() }
    @Test fun en_program() = capture("5-program", "en") { Program() }
    @Test fun en_pass() = capture("6-pass", "en") { Pass() }
    @Test fun en_events() = capture("7-events", "en", navTab = 2) { Events() }
    @Test fun en_profile() = capture("8-profile", "en") { Profile() }
    @Test fun en_welcome() = capture("9-welcome", "en") { WelcomeScreen() }

    // --------------------------------------------------------------------- thai

    @Test fun th_home() = capture("1-home", "th", navTab = 0) { Home() }
    @Test fun th_booths() = capture("2-booths", "th", navTab = 1) { Booths() }
    @Test fun th_scan() = capture("3-scan", "th") { Scan() }
    @Test fun th_prizes() = capture("4-prizes", "th") { Prizes() }
    @Test fun th_program() = capture("5-program", "th") { Program() }
    @Test fun th_pass() = capture("6-pass", "th") { Pass() }
    @Test fun th_events() = capture("7-events", "th", navTab = 2) { Events() }
    @Test fun th_profile() = capture("8-profile", "th") { Profile() }
    @Test fun th_welcome() = capture("9-welcome", "th") { WelcomeScreen() }

    // ----------------------------------------------------------------- the cast

    /**
     * A student who does not exist, with an id no intake will ever issue in that
     * shape. See the class comment: these frames end up on a public listing.
     */
    private val DemoStudent = PreviewStudent.copy(
        firstName = "Nampeung",
        surname = "Kittisak",
        email = "6900000000@lamduan.mfu.ac.th",
        studentId = "6900000000",
        phone = "0800000000",
    )

    /**
     * The wall clock, not a fixture.
     *
     * `previewProgram` lays its entries out relative to whatever it is given —
     * one finished, one running, two ahead — but the screen decides what to badge
     * as "Happening now" against the *device's* clock. Hand it a frozen
     * timestamp and the two disagree: the first run of this put the running order
     * three hours before an entry it had already ticked off.
     */
    private val now get() = System.currentTimeMillis()

    @Composable private fun Home() = HomeScreen(
        student = DemoStudent,
        progress = PreviewProgress,
        program = previewProgram(now),
    )

    @Composable private fun Booths() = BoothsScreen(
        booths = previewRoster(7),
        zones = PreviewZones,
    )

    @Composable private fun Scan() = ScanScreen(
        outcome = ScanOutcome.Recorded(previewRoster(7)[11]),
    )

    @Composable private fun Prizes() = PrizesScreen(
        progress = PreviewProgress,
        student = DemoStudent,
    )

    @Composable private fun Program() = ProgramScreen(program = previewProgram(now))

    @Composable private fun Pass() = QrTicketScreen(student = DemoStudent)

    @Composable private fun Events() = EventsScreen()

    @Composable private fun Profile() = ProfileScreen(student = DemoStudent)
}
