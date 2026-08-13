package com.su.clubfair.ui.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.theme.AlanSans

/**
 * "By continuing, you agree to…", with both documents reachable from it.
 *
 * The sentence existed before this and the documents did not, which is the part
 * worth naming: sign-up asked a student to agree to two policies, underlined
 * their names so they looked like links, and gave them nothing to open — first
 * because they were plain text, then because the addresses behind them were
 * never published. Consent to an unreadable document is not consent, and it is
 * the sort of thing that is only ever noticed by the person it was taken from.
 *
 * The underlined halves are real links: [LinkAnnotation.Clickable] rather than a
 * tap on the whole paragraph, so the two names are separately tappable and
 * TalkBack announces them as links instead of reading one long sentence with no
 * way into either.
 *
 * The phrases are located by searching the sentence for them rather than by
 * splitting on a placeholder, so a translation is free to reorder the two, or to
 * name only one of them, and the worst case is a line that reads correctly and
 * has nothing to tap.
 */
@Composable
fun LegalConsentNotice(
    onOpen: (LegalDocument) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes notice: Int = R.string.register_legal_continue,
) {
    Text(
        text = consentText(notice = notice, onOpen = onOpen),
        modifier = modifier.fillMaxWidth(),
        fontFamily = AlanSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 1.45.em,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun consentText(
    @StringRes notice: Int,
    onOpen: (LegalDocument) -> Unit,
): AnnotatedString {
    val sentence = stringResource(notice)
    val phrases = listOf(
        stringResource(R.string.register_legal_terms) to LegalDocument.Terms,
        stringResource(R.string.register_legal_privacy) to LegalDocument.Privacy,
    )

    // Underlined and a shade heavier, which is how these read before they were
    // tappable — the styling was never the problem.
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
        ),
    )

    return buildAnnotatedString {
        var cursor = 0
        // In the order they appear in the sentence, not the order they are listed
        // here: a translation that puts the privacy policy first would otherwise
        // have its spans laid down back to front and lose one.
        phrases
            .mapNotNull { (phrase, document) ->
                sentence.indexOf(phrase).takeIf { it >= 0 }?.let { Triple(it, phrase, document) }
            }
            .sortedBy { (at, _, _) -> at }
            .forEach { (at, phrase, document) ->
                if (at < cursor) return@forEach
                append(sentence.substring(cursor, at))
                withLink(
                    LinkAnnotation.Clickable(
                        tag = document.name,
                        styles = linkStyle,
                        linkInteractionListener = { onOpen(document) },
                    ),
                ) {
                    append(phrase)
                }
                cursor = at + phrase.length
            }
        append(sentence.substring(cursor))
    }
}
