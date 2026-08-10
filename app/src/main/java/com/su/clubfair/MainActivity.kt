package com.su.clubfair

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.su.clubfair.ui.AppShell
import com.su.clubfair.ui.FairViewModel
import com.su.clubfair.ui.SessionState
import com.su.clubfair.ui.auth.AuthViewModel
import com.su.clubfair.ui.auth.LoginScreen
import com.su.clubfair.ui.auth.RegisterGoogleScreen
import com.su.clubfair.ui.auth.RegisterScreen
import com.su.clubfair.ui.onboarding.OnboardingScreen
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.SUClubFairTheme
import com.su.clubfair.ui.welcome.WelcomeScreen

/**
 * The screens reachable before sign-in.
 *
 * Declaration order is the forward order of the flow — the transition below
 * reads the ordinals to tell "going deeper" from "going back", so keep them in
 * sequence. Onboarding and Home have left this list: both are now decided by
 * whether a session exists rather than by where the student last tapped, which
 * is what makes a login survive closing the app.
 */
private enum class AuthStep {
    Welcome,
    Login,
    RegisterGoogle,
    Register,
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
        is SessionState.SignedIn ->
            if (current.onboardingSeen) AppPhase.Shell else AppPhase.Onboarding
    }

    Crossfade(
        targetState = phase,
        animationSpec = tween(TransitionMillis),
        label = "phase",
    ) { current ->
        when (current) {
            // Deliberately bare. Anything more is a splash screen, and a splash
            // screen for a DataStore read is an animation in front of nothing.
            AppPhase.Restoring -> Box(Modifier.fillMaxSize()) { MeshBackground() }
            AppPhase.SignedOut -> SignedOutFlow()
            AppPhase.Onboarding -> OnboardingScreen(onContinue = fair::markOnboardingSeen)
            AppPhase.Shell -> AppShell(onSignOut = fair::signOut)
        }
    }
}

/** What the app is showing at the top level — see [ClubFairApp]. */
private enum class AppPhase { Restoring, SignedOut, Onboarding, Shell }

/** Welcome, sign-in and sign-up, with the slide between them. */
@Composable
private fun SignedOutFlow() {
    val auth: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val loginForm by auth.login.collectAsStateWithLifecycle()
    val registerForm by auth.register.collectAsStateWithLifecycle()
    val hasAccount by auth.hasAccount.collectAsStateWithLifecycle()

    var step by rememberSaveable { mutableStateOf(AuthStep.Welcome) }

    // Back rewinds the sign-in steps. It no longer needs to exclude onboarding —
    // that is not in this flow any more.
    BackHandler(enabled = step != AuthStep.Welcome) {
        step = when (step) {
            AuthStep.Register -> AuthStep.RegisterGoogle
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
                // No navigation on success: writing the session makes
                // `SessionState` change, and the gate above swaps the whole
                // tree. A screen that navigated itself as well would be a
                // second source of truth for the same fact.
                onSubmit = { auth.submitLogin(onSuccess = {}) },
                onSignUp = { step = AuthStep.RegisterGoogle },
                // Lands where the password form lands. There is no Google flow
                // behind it yet — no Credential Manager call, no ID token,
                // nothing to verify it against — so it goes to the form that
                // collects the same facts by asking.
                onGoogleLogin = { step = AuthStep.RegisterGoogle },
            )

            AuthStep.RegisterGoogle -> RegisterGoogleScreen(
                onGoogleContinue = { step = AuthStep.Register },
                onLogin = { step = AuthStep.Login },
            )

            AuthStep.Register -> RegisterScreen(
                state = registerForm,
                onChange = auth::onRegisterField,
                onCreateAccount = { auth.submitRegister(onSuccess = {}) },
                onLogin = { step = AuthStep.Login },
                replacesExistingAccount = hasAccount,
            )
        }
    }
}
