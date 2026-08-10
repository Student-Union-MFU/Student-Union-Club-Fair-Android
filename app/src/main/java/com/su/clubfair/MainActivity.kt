package com.su.clubfair

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.su.clubfair.ui.auth.LoginScreen
import com.su.clubfair.ui.auth.RegisterGoogleScreen
import com.su.clubfair.ui.auth.RegisterScreen
import com.su.clubfair.ui.AppShell
import com.su.clubfair.ui.onboarding.OnboardingScreen
import com.su.clubfair.ui.theme.SUClubFairTheme
import com.su.clubfair.ui.welcome.WelcomeScreen

/**
 * The screens reachable before sign-in, plus the onboarding card that follows it.
 *
 * Declaration order is the forward order of the flow — the transition below reads
 * the ordinals to tell "going deeper" from "going back", so keep them in sequence.
 */
private enum class Screen {
    Welcome,
    Login,
    RegisterGoogle,
    Register,
    Onboarding,
    Home,
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
            // Flat state-based routing — enough until there's a post-login graph.
            var screen by rememberSaveable { mutableStateOf(Screen.Welcome) }

            SUClubFairTheme {

                // Back only rewinds the sign-in steps; onboarding is one-way.
                BackHandler(
                    enabled = screen == Screen.Login ||
                        screen == Screen.RegisterGoogle ||
                        screen == Screen.Register,
                ) {
                    screen = when (screen) {
                        Screen.Register -> Screen.RegisterGoogle
                        Screen.RegisterGoogle -> Screen.Login
                        else -> Screen.Welcome
                    }
                }

                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        // Deeper in the flow slides left; backwards slides right.
                        val forward = targetState.ordinal > initialState.ordinal
                        val direction = if (forward) 1 else -1
                        val slide = tween<IntOffset>(
                            durationMillis = TransitionMillis,
                            easing = FastOutSlowInEasing,
                        )
                        // Slide only, no crossfade: every screen carries the same
                        // backdrop, so the two tile exactly while they move —
                        // fading either one would let the window background flash
                        // through instead.
                        slideInHorizontally(slide) { width -> direction * width }
                            .togetherWith(
                                slideOutHorizontally(slide) { width -> -direction * width }
                            ).using(
                            // Both screens are full-bleed; clipping would cut the
                            // gradient off mid-slide.
                            SizeTransform(clip = false)
                        )
                    },
                    label = "screen",
                ) { target ->
                    when (target) {
                        Screen.Welcome -> WelcomeScreen(
                            onGetStarted = { screen = Screen.Login },
                        )

                        Screen.Login -> LoginScreen(
                            onLogin = { _, _ -> screen = Screen.Onboarding },
                            onSignUp = { screen = Screen.RegisterGoogle },
                            // Lands where the password form lands. There is no
                            // Google flow behind it yet — no Credential Manager
                            // call, no ID token, nothing to verify it against —
                            // and this stays a stub until there is an account
                            // system for it to authenticate to.
                            onGoogleLogin = { screen = Screen.Onboarding },
                        )

                        // Google first, then the form for what Google can't
                        // supply. Same stub as the login screen's button: this
                        // advances the flow without a Credential Manager call, an
                        // ID token, or anything to verify one against.
                        Screen.RegisterGoogle -> RegisterGoogleScreen(
                            onGoogleContinue = { screen = Screen.Register },
                            onLogin = { screen = Screen.Login },
                        )

                        Screen.Register -> RegisterScreen(
                            onCreateAccount = { screen = Screen.Onboarding },
                            onLogin = { screen = Screen.Login },
                        )

                        Screen.Onboarding -> OnboardingScreen(
                            onContinue = { screen = Screen.Home },
                        )

                        Screen.Home -> AppShell(
                            // Sign-out rewinds to the very start of the flow.
                            onSignOut = { screen = Screen.Welcome },
                        )
                    }
                }
            }
        }
    }
}
