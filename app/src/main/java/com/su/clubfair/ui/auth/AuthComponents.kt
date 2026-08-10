package com.su.clubfair.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.data.PasswordPolicy
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.Palette

/**
 * Shared chrome for the login / register screens: the backdrop and the glass
 * form controls that sit on top of it.
 * The white pill CTA lives in [com.su.clubfair.ui.components.PillButton], since
 * onboarding uses it too.
 */

internal val FieldShape = RoundedCornerShape(Dimens.RadiusMd)
internal val FieldHeight = 56.dp
internal val ScreenPadding = Dimens.ScreenPadding

/**
 * The glass under each field — see [com.su.clubfair.ui.components.glassSurface]
 * for how the material itself is built.
 *
 * These panes carry no backdrop blur. They sit on a smooth mesh gradient, and
 * blurring a smooth gradient hands back very nearly the same gradient — and a
 * blur pass *per field* on a form of eight would be the wrong place to spend it
 * regardless. At field size the frost fill separates them on its own. The nav
 * bar is the one surface that earns a real material; see `GlassNavBar`.
 */
@Composable
private fun Modifier.glassField(focus: Float): Modifier =
    glassSurface(cornerRadius = Dimens.RadiusMd, intensity = focus)

/** Drives [glassField] from a field's focus state, easing rather than snapping. */
@Composable
private fun rememberFocusRamp(focused: Boolean): Float {
    val focus by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "fieldFocus",
    )
    return focus
}

/** Full-bleed backdrop shared by both auth screens, with edge-to-edge insets applied. */
@Composable
fun AuthBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        MeshBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = ScreenPadding),
            content = content,
        )
    }
}

@Composable
private fun authFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = Color.White,
    // The outline is drawn by glassField as part of the rim; Material's own
    // border would sit on top of it as a second, flatter edge.
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedPlaceholderColor = Ink.Placeholder,
    unfocusedPlaceholderColor = Ink.Placeholder,
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Ink.Placeholder,
)

private val FieldTextStyle = TextStyle(
    fontFamily = AlanSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
)

/**
 * The copy for one rejected field.
 *
 * The mapping lives here rather than in `AuthViewModel` so the ViewModel never
 * has to hold a `Context` — and so a Thai translation of "at least 8 characters"
 * is a resource change rather than a Kotlin one.
 */
@Composable
fun fieldErrorText(error: FieldError): String = when (error) {
    FieldError.Required -> stringResource(R.string.auth_error_required)
    FieldError.BadPhone -> stringResource(R.string.auth_error_phone)
    FieldError.BadEmail -> stringResource(R.string.auth_error_email)
    FieldError.NotMfuEmail -> stringResource(R.string.auth_error_email_mfu)
    FieldError.PasswordTooShort ->
        stringResource(R.string.auth_error_password_short, PasswordPolicy.MinLength)

    FieldError.PasswordNeedsLetter -> stringResource(R.string.auth_error_password_letter)
    FieldError.PasswordNeedsDigit -> stringResource(R.string.auth_error_password_digit)
}

/**
 * A form field, with its complaint underneath it when it has one.
 *
 * [error] is what the field thinks; [showError] is whether the form is ready to
 * say so. Keeping those apart is what stops the screen marking a password field
 * red on the first keystroke — see `LoginForm.showErrors`.
 *
 * The message is attached to the field with `semantics { error(...) }` as well
 * as drawn under it, so TalkBack announces the reason on focus rather than
 * reading a field that gives no hint why it was rejected.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false,
    error: FieldError? = null,
    showError: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    // Per-field, not hoisted: revealing one password should not reveal the next.
    var revealed by remember { mutableStateOf(false) }

    val visible = error.takeIf { showError }
    val message = visible?.let { fieldErrorText(it) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FieldHeight)
                .glassField(rememberFocusRamp(focused))
                .then(
                    // A rim in the alert colour, so a rejected field is legible
                    // as rejected before the message under it is read — and at a
                    // glance down a form of seven.
                    if (message != null) {
                        Modifier.border(1.dp, Palette.Alert.copy(alpha = 0.7f), FieldShape)
                    } else {
                        Modifier
                    },
                )
                .semantics { if (message != null) error(message) },
            interactionSource = interactionSource,
            placeholder = { Text(text = placeholder, style = FieldTextStyle) },
            textStyle = FieldTextStyle,
            singleLine = true,
            isError = message != null,
            shape = FieldShape,
            colors = authFieldColors(),
            keyboardOptions = keyboardOptions,
            trailingIcon = if (isPassword) {
                {
                    PasswordToggle(
                        revealed = revealed,
                        onToggle = { revealed = !revealed },
                    )
                }
            } else {
                null
            },
            visualTransformation = when {
                isPassword && !revealed -> PasswordVisualTransformation()
                else -> VisualTransformation.None
            },
        )

        if (message != null) {
            Text(
                text = message,
                modifier = Modifier
                    .padding(start = Dimens.CardPadding, top = Dimens.SpaceXs)
                    // Already announced through the field's own error semantics;
                    // reading it twice is how a form gets tedious under TalkBack.
                    .clearAndSetSemantics {},
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Palette.Alert,
            )
        }
    }
}

/**
 * The reveal control inside a password field.
 *
 * 48dp of touch target around a 20dp glyph. Material's `IconButton` is exactly
 * this and would be shorter, but it also brings its own 40dp ripple and its own
 * content colour, both of which fight the field's glass.
 */
@Composable
private fun PasswordToggle(
    revealed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                if (revealed) R.drawable.ic_eye_off else R.drawable.ic_eye,
            ),
            contentDescription = stringResource(
                if (revealed) R.string.auth_hide_password else R.string.auth_show_password,
            ),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * What went wrong with the form as a whole, after a submit.
 *
 * A card above the fields rather than a toast: a wrong password is not
 * incidental information that can be allowed to disappear on a timer while
 * someone is still reading the field it refers to.
 */
@Composable
fun AuthFormError(error: FormError, modifier: Modifier = Modifier) {
    // su-server's own message wins when it sent one: only the server knows whether
    // it was the number, the password or a flagged account, and it phrases that in
    // the reader's language already. The resource is the fallback.
    val message = when (error) {
        is FormError.Rejected -> error.message ?: stringResource(R.string.login_error_generic)
        FormError.Offline -> stringResource(R.string.auth_error_offline)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(Palette.Alert.copy(alpha = 0.14f))
            .border(1.dp, Palette.Alert.copy(alpha = 0.45f), FieldShape)
            .padding(horizontal = Dimens.CardPadding, vertical = Dimens.Space)
            // One announcement for the pair, and it interrupts: this arrives
            // after a submit, when focus is nowhere near it.
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = Palette.Alert,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Dimens.Space))
        Text(
            text = message,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Palette.Alert,
        )
    }
}

/**
 * A hairline rule with a word sitting in the gap — "or", between the two ways in.
 *
 * The rules are [Ink.Faint] and the word [Ink.Muted]: a separator that reads as
 * loudly as the controls it separates turns a form into a stack of sections.
 */
@Composable
fun AuthDivider(
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Ink.Faint)
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = Dimens.Space),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Ink.Muted,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Ink.Faint)
    }
}

/**
 * The federated sign-in option, sitting under the password CTA.
 *
 * Glass rather than the solid white Google's guidelines lead with. Two reasons:
 * the accent pill above it is the primary action and a second opaque button
 * beside it would read as an equal choice, and a white slab is the one thing on
 * these screens that would sit *on* the backdrop instead of in it. The mark
 * itself is untouched — full colour, unmodified, never re-drawn in the app's
 * palette, which is the part of the guidelines that actually matters.
 */
@Composable
fun GoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FieldHeight)
            // Clip before the ripple goes on, or it spreads square out of a pill.
            .clip(CircleShape)
            .glassSurface(shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.CardPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            // Google's mark stays full colour and unmodified when the button is
            // live — that is the part of their guidelines that matters — and only
            // fades with the rest of the control when it is not.
            alpha = if (enabled) 1f else 0.4f,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Dimens.Space))
        Text(
            text = text,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = if (enabled) Color.White else Ink.Faint,
        )
    }
}

/**
 * Read-only field that opens a menu of [options]; used for School / Major.
 * Styled to match [AuthTextField] so the form reads as one set of controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    error: FieldError? = null,
    showError: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val message = error.takeIf { showError }?.let { fieldErrorText(it) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = FieldHeight)
                // Read-only, so it never takes text focus — the open menu is what
                // makes it the active control.
                .glassField(rememberFocusRamp(expanded))
                .then(
                    if (message != null) {
                        Modifier.border(1.dp, Palette.Alert.copy(alpha = 0.7f), FieldShape)
                    } else {
                        Modifier
                    },
                )
                .semantics { if (message != null) error(message) },
            placeholder = { Text(text = placeholder, style = FieldTextStyle) },
            textStyle = FieldTextStyle,
            singleLine = true,
            isError = message != null,
            shape = FieldShape,
            colors = authFieldColors(),
            supportingText = message?.let {
                {
                    Text(
                        text = it,
                        modifier = Modifier.clearAndSetSemantics {},
                        fontFamily = AlanSans,
                        fontSize = 12.sp,
                        color = Palette.Alert,
                    )
                }
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        // The menu is the one surface here that can't be glass: it opens over
        // the form's own fields, so a translucent panel would show the field
        // it's covering straight through its options. Opaque, and taken from the
        // scheme's one panel tone so it still belongs to the backdrop.
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Palette.Panel,
            shape = RoundedCornerShape(16.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option, style = FieldTextStyle) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = Color.White),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
