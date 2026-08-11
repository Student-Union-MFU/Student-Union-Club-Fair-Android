package com.su.clubfair

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.su.clubfair.ui.AppShell
import com.su.clubfair.ui.FairViewModel
import com.su.clubfair.ui.ProvideAppLanguage
import com.su.clubfair.ui.SessionState
import com.su.clubfair.data.net.GoogleSignIn
import com.su.clubfair.ui.auth.AuthViewModel
import com.su.clubfair.ui.auth.CompleteProfileScreen
import com.su.clubfair.ui.auth.FormError
import com.su.clubfair.ui.auth.LoginScreen
import com.su.clubfair.ui.auth.RegisterGoogleScreen
import com.su.clubfair.ui.loading.LoadingScreen
import com.su.clubfair.ui.model.Student
import com.su.clubfair.ui.onboarding.OnboardingScreen
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.SUClubFairTheme
import com.su.clubfair.ui.welcome.WelcomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The screens reachable before sign-in.
 *
 * Declaration order is the forward order of the flow — the transition below
 * reads the ordinals to tell "going deeper" from "going back", so keep them in
 * sequence. Onboarding and Home have left this list: both are now decided by
 * whether a session exists rather than by where the student last tapped, which
 * is what makes a login survive closing the app.
 *
 * `RegisterGoogle` is back, and is now what "Sign up" opens. It was dropped when
 * the Google button had no client id behind it — a step that did nothing in
 * front of a form that asked for everything anyway. There is a client id now, so
 * the order it was written for holds again: identity first, from someone who can
 * verify it, then only the fields Google cannot answer.
 *
 * `Register` — the email form — is *not* in this list. Sign-up is Google and
 * nothing else: every account is an MFU one, so the address establishes the
 * student id and Google is the only party that can vouch for it. The screen file
 * survives, unreferenced, on the same terms `RegisterGoogleScreen` did while it
 * was out: deleting it would take `RegisterForm`, `submitRegister` and
 * `POST /auth/register` with it, and the server still answers that route.
 */
private enum class AuthStep {
    Welcome,
    Login,
    RegisterGoogle,
}

/** Slide duration between screens. */
private const val TransitionMillis = 380

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The backdrop is dark on every screen, so force light (white) system bar
        // icons rather than letting them follow the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            SUClubFairTheme {
                ClubFairApp()
            }
        }
    }
}

/**
 * The one gate in the app: is anyone signed in?
 *
 * Routing used to be a single `Screen` enum that a tap moved through, which
 * meant "signed in" was a position in an animation rather than a fact — closing
 * the app on Home reopened it on Welcome, and there was nothing for a session to
 * be restored *into*.
 *
 * [SessionState.Restoring] is the state that makes persistence bearable. Reading
 * DataStore is asynchronous, so for the first frames after launch the app does
 * not know whether anyone is signed in. Rendering Welcome during that window is
 * what makes a returning student watch the sign-in screen flash past on every
 * cold start; the backdrop alone is shown instead, which is what every screen
 * stands on anyway, so the arrival reads as one continuous surface.
 */
@Composable
private fun ClubFairApp() {
    val fair: FairViewModel = viewModel(factory = FairViewModel.Factory)
    val session by fair.session.collectAsStateWithLifecycle()
    val contentReady by fair.contentReady.collectAsStateWithLifecycle()
    val language by fair.language.collectAsStateWithLifecycle()

    // The crossfade keys on the *phase*, not on the session value.
    //
    // Keying it on `session` directly looks equivalent and is not: `SignedIn`
    // carries the student, so every scan produces a new instance, `Crossfade`
    // reads that as a different screen, and the whole signed-in tree is torn
    // down and rebuilt. In practice that meant collecting a booth threw you back
    // to the Home tab, because `AppShell`'s `rememberSaveable` tab index was
    // discarded along with it. Four values, and only a move between them is a
    // change of screen.
    val phase = when (val current = session) {
        SessionState.Restoring -> AppPhase.Restoring
        SessionState.SignedOut -> AppPhase.SignedOut
        is SessionState.SignedIn -> when {
            // A Google sign-in creates the account before the student has given a
            // password, a phone number, a school or a major — so a session can
            // exist for someone who has not finished signing up. They get the
            // rest of the form, ahead of onboarding: the account is the thing
            // they are in the middle of, and an intro to the fair on top of a
            // half-made account is an interruption of it.
            !current.student.profileComplete -> AppPhase.CompletingSignUp
            !current.onboardingSeen -> AppPhase.Onboarding
            // Onboarding first, and the wait after it. The intro needs nothing
            // from the network, so making a new student watch a loading bar
            // before it would be a wait invented for its own sake — and by the
            // time they have read it the fetch has usually landed anyway.
            !contentReady -> AppPhase.Preparing
            else -> AppPhase.Shell
        }
    }

    // Held across the crossfade. Signing out flips the session to `SignedOut`
    // immediately, while the outgoing frame of the screen being faded away still
    // has to draw a student — without this it draws a blank one for the length
    // of the transition.
    val lastSignedIn = remember { mutableStateOf<SessionState.SignedIn?>(null) }
    (session as? SessionState.SignedIn)?.let { lastSignedIn.value = it }

    // Above the gate, so every screen in the app is inside it — the sign-in
    // forms included. A student who cannot read the sign-in screen cannot reach
    // the setting that would fix it, so the language has to apply before there
    // is an account to attach it to.
    ProvideAppLanguage(language) {
        Crossfade(
            targetState = phase,
            animationSpec = tween(TransitionMillis),
            label = "phase",
        ) { current ->
            when (current) {
                // Deliberately bare, and still bare now that there is a real
                // loading screen a branch below. Anything more is a splash
                // screen, and a splash screen for a DataStore read is an
                // animation in front of nothing: this phase is over in a frame
                // or two, and the wordmark would be gone before it had drawn.
                AppPhase.Restoring -> Box(Modifier.fillMaxSize()) { MeshBackground() }
                AppPhase.SignedOut -> SignedOutFlow()
                AppPhase.CompletingSignUp -> lastSignedIn.value?.let { signedIn ->
                    CompleteSignUpFlow(student = signedIn.student, onSignOut = fair::signOut)
                }

                AppPhase.Onboarding -> OnboardingScreen(onContinue = fair::markOnboardingSeen)
                AppPhase.Preparing -> LoadingScreen()
                AppPhase.Shell -> AppShell(onSignOut = fair::signOut)
            }
        }
    }
}

/**
 * What the app is showing at the top level — see [ClubFairApp].
 *
 * [Preparing] is the gap a session leaves behind it: signed in, past onboarding,
 * and the first fetch made with the new token has not landed. Its own phase
 * rather than an empty shell, because the shell would draw a booth list of none
 * and a rank of nothing and then quietly rearrange itself.
 */
private enum class AppPhase { Restoring, SignedOut, CompletingSignUp, Onboarding, Preparing, Shell }

/**
 * The rest of sign-up for an account Google has just created.
 *
 * Its own composable so the form's ViewModel is scoped to the phase: the moment
 * `profile_complete` flips, the gate drops this branch and the half-typed state
 * goes with it, rather than lingering for the rest of the process.
 *
 * No `BackHandler`. There is nothing behind this screen — the account already
 * exists — so back would have to mean either "do nothing" or "sign out", and a
 * gesture that silently signs someone out is worse than one that does nothing.
 * The sign-out is on the screen instead, where it says what it does.
 */
@Composable
private fun CompleteSignUpFlow(student: Student, onSignOut: () -> Unit) {
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val form by auth.completeProfile.collectAsStateWithLifecycle()

    // Seeds once; a background refresh of the session must not overwrite a form
    // the student is halfway through. See `seedCompleteProfile`.
    LaunchedEffect(student.id) { auth.seedCompleteProfile(student) }

    CompleteProfileScreen(
        state = form,
        onChange = auth::onCompleteProfileField,
        onSubmit = auth::submitCompleteProfile,
        onSignOut = onSignOut,
    )
}

/** Welcome, sign-in and sign-up, with the slide between them. */
@Composable
private fun SignedOutFlow() {
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val loginForm by auth.login.collectAsStateWithLifecycle()

    // Credential Manager shows UI, so it needs the Activity rather than the
    // application context.
    val activity = LocalActivity.current

    var step by rememberSaveable { mutableStateOf(AuthStep.Welcome) }

    // Back rewinds the sign-in steps. It no longer needs to exclude onboarding —
    // that is not in this flow any more.
    BackHandler(enabled = step != AuthStep.Welcome) {
        step = when (step) {
            AuthStep.RegisterGoogle -> AuthStep.Login
            else -> AuthStep.Welcome
        }
    }

    AnimatedContent(
        targetState = step,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            // Deeper in the flow slides left; backwards slides right.
            val forward = targetState.ordinal > initialState.ordinal
            val direction = if (forward) 1 else -1
            val slide = tween<IntOffset>(
                durationMillis = TransitionMillis,
                easing = FastOutSlowInEasing,
            )
            // Slide only, no crossfade: every screen carries the same backdrop,
            // so the two tile exactly while they move — fading either one would
            // let the window background flash through instead.
            slideInHorizontally(slide) { width -> direction * width }
                .togetherWith(
                    slideOutHorizontally(slide) { width -> -direction * width }
                ).using(
                // Both screens are full-bleed; clipping would cut the gradient
                // off mid-slide.
                SizeTransform(clip = false)
            )
        },
        label = "authStep",
    ) { target ->
        when (target) {
            AuthStep.Welcome -> WelcomeScreen(
                onGetStarted = { step = AuthStep.Login },
            )

            AuthStep.Login -> LoginScreen(
                state = loginForm,
                onPhoneChange = auth::onLoginPhone,
                onPasswordChange = auth::onLoginPassword,
                // No navigation on success: a successful sign-in writes the
                // session, `SessionState` changes, and the gate above swaps the
                // whole tree. A screen that navigated itself as well would be a
                // second source of truth for one fact.
                onSubmit = auth::submitLogin,
                onSignUp = { step = AuthStep.RegisterGoogle },
                onGoogleLogin = { requestGoogleSignIn(activity, auth) },
                googleAvailable = auth.googleAvailable,
            )

            // Google here does the same thing it does on the login screen, and
            // that is not a duplicate: `auth/google` creates the account if it
            // is not there and returns a session if it is, so "sign in" and
            // "sign up" are one call. What differs is where the student lands
            // afterwards, and the gate above decides that from
            // `profile_complete` rather than from which button was pressed.
            AuthStep.RegisterGoogle -> RegisterGoogleScreen(
                state = loginForm,
                googleAvailable = auth.googleAvailable,
                onGoogleContinue = { requestGoogleSignIn(activity, auth) },
                onLogin = { step = AuthStep.Login },
            )
        }
    }
}

/**
 * Opens the Google credential sheet and hands the ID token to the ViewModel.
 *
 * Disabled unless a Web OAuth client id was built in — see
 * `GoogleSignIn.isConfigured`, and `localConfig` in the app's build file for
 * where that id comes from. A build without one keeps the button visibly off
 * rather than opening a sheet that cannot succeed.
 *
 * A student who has never used the app before ends up signed in to an account
 * that exists but is not finished; the gate in [ClubFairApp] sends them to
 * [CompleteProfileScreen] rather than into the fair.
 *
 * The app never inspects the token. su-server verifies the signature, the
 * audience, `email_verified` and the MFU domain; a client-side check would prove
 * nothing, since a client can be modified. The profile beside it — name, photo,
 * address — is passed on too, and is only ever used to fill in what the server
 * has no copy of. See `GoogleAccount`.
 */
private fun requestGoogleSignIn(activity: android.app.Activity?, auth: AuthViewModel) {
    val host = activity ?: return
    // The ViewModel's own scope, so a rotation mid-sheet does not orphan the call.
    CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        when (val result = GoogleSignIn.requestIdToken(host)) {
            is GoogleSignIn.Result.Token -> auth.submitGoogle(result.idToken, result.account)
            // Dismissing the sheet is not a failure. Say nothing.
            GoogleSignIn.Result.Cancelled -> Unit
            GoogleSignIn.Result.NoAccount ->
                auth.onGoogleFailed(FormError.GoogleNoAccount)
            // Both mean "this could not have worked", and neither is anything the
            // student can fix — so they get the same message, which points at the
            // password field rather than pretending to diagnose the console.
            GoogleSignIn.Result.NotConfigured,
            is GoogleSignIn.Result.Failed,
            -> auth.onGoogleFailed(FormError.GoogleUnavailable)
        }
    }
}
