package com.su.clubfair.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.BuildConfig
import com.su.clubfair.R
import com.su.clubfair.data.Links
import com.su.clubfair.ui.components.ActionRow
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.InfoRow
import com.su.clubfair.ui.components.SectionLabel
import com.su.clubfair.ui.components.SheetHeader
import com.su.clubfair.ui.components.ToggleRow
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

private val CardRadius = Dimens.RadiusLg

/**
 * Settings, opened from Profile.
 *
 * The app had none, which left three things with nowhere to live: a preference
 * anyone would want to turn off, the version number every bug report starts
 * with, and — the one that matters — an honest statement of where a student's
 * checkpoints actually are.
 *
 * That last one is why the storage card is not a footnote. Progress is held on
 * this phone and nowhere else, and a student who assumes otherwise will find out
 * by losing an afternoon of scanning. Saying so is not an apology for the
 * missing server; it is the only thing that makes the current behaviour safe to
 * rely on.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true,
    /** Scans taken offline that the server has not accepted yet. */
    pendingScans: Int = 0,
    offline: Boolean = false,
    onHapticsChange: (Boolean) -> Unit = {},
    onEraseDevice: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    var confirmingErase by remember { mutableStateOf(false) }

    val noBrowser = stringResource(R.string.settings_no_browser)
    val openLink: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // A phone with no browser is rare and not the app's problem to solve,
            // but a tap that does nothing at all is indistinguishable from a bug.
            Toast.makeText(context, noBrowser, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        SheetHeader(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
            backDescription = stringResource(R.string.profile_back),
        )

        Spacer(Modifier.height(Dimens.SpaceLg))
        SectionLabel(stringResource(R.string.settings_feedback_section))
        Spacer(Modifier.height(Dimens.SpaceSm))
        Card {
            ToggleRow(
                icon = R.drawable.ic_vibrate,
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_hint),
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChange,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceLg))
        SectionLabel(stringResource(R.string.settings_data_section))
        Spacer(Modifier.height(Dimens.SpaceSm))
        Card {
            Column(modifier = Modifier.padding(vertical = Dimens.Space)) {
                Text(
                    text = stringResource(R.string.settings_stored_title),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = if (pendingScans > 0) {
                        pluralStringResource(
                            R.plurals.settings_pending_scans,
                            pendingScans,
                            pendingScans,
                        )
                    } else {
                        stringResource(
                            if (offline) R.string.settings_synced_offline
                            else R.string.settings_synced,
                        )
                    },
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    // Amber while something is still waiting to go up, accent once
                    // everything has: the colour is the answer to "am I safe?".
                    color = if (pendingScans > 0) Palette.Alert else Palette.Accent,
                )
                Spacer(Modifier.height(Dimens.SpaceXs))
                Text(
                    text = stringResource(R.string.settings_stored_body),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 1.45.em,
                    color = Ink.Muted,
                )
            }
            Hairline()
            ActionRow(
                icon = R.drawable.ic_trash,
                title = stringResource(R.string.settings_erase),
                subtitle = stringResource(R.string.settings_erase_hint),
                tint = Palette.Alert,
                onClick = { confirmingErase = true },
            )
        }

        Spacer(Modifier.height(Dimens.SpaceLg))
        SectionLabel(stringResource(R.string.settings_about_section))
        Spacer(Modifier.height(Dimens.SpaceSm))
        Card {
            InfoRow(
                icon = R.drawable.ic_badge,
                title = stringResource(R.string.settings_version),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
            Hairline()
            ActionRow(
                icon = R.drawable.ic_layers,
                title = stringResource(R.string.settings_terms),
                external = true,
                onClick = { openLink(Links.Terms) },
            )
            Hairline()
            ActionRow(
                icon = R.drawable.ic_lock,
                title = stringResource(R.string.settings_privacy),
                external = true,
                onClick = { openLink(Links.Privacy) },
            )
        }

        Spacer(Modifier.height(Dimens.SpaceXl))
    }

    if (confirmingErase) {
        EraseConfirmation(
            onConfirm = {
                confirmingErase = false
                onEraseDevice()
            },
            onDismiss = { confirmingErase = false },
        )
    }
}

/**
 * The one destructive action in the app, behind the one dialog in the app.
 *
 * A dialog rather than an undo snackbar, because there is nothing to undo *to*:
 * the scans are not held anywhere else, so a mistaken tap is unrecoverable and
 * has to be stopped before it happens rather than apologised for afterwards.
 */
@Composable
private fun EraseConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Panel,
        title = {
            Text(
                text = stringResource(R.string.settings_erase_confirm_title),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_erase_confirm_body),
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 1.45.em,
                color = Ink.Label,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_erase_confirm_cta),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Palette.Alert,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_cancel),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Ink.Label,
                )
            }
        },
    )
}

/** The glass card every group of rows sits in. */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = CardRadius)
            .padding(horizontal = Dimens.CardPadding),
    ) {
        content()
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun SettingsPreview() {
    SUClubFairTheme {
        androidx.compose.foundation.layout.Box {
            MeshBackground()
            SettingsScreen(pendingScans = 2)
        }
    }
}
