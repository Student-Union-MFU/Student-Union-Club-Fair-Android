package com.su.clubfair.ui.booths

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.Booth
import com.su.clubfair.ui.model.previewRoster
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * Search across every club at the fair.
 *
 * The Booths tab is built around walking: pick the area you are standing in, get
 * its nine booths. That is right for a student wandering, and useless for one
 * who has been told "come find the Robotics Club" — the areas are habitats, so
 * there is no way to reason from a club's name to which of the three it sits in.
 *
 * Matching is over the club's name *and* its blurb, which is why "code" finds
 * the Coding Club and "photo" finds both Photography and Film. A name-only
 * search on 27 items would mostly be a slower way to scroll.
 */
@Composable
fun BoothSearch(
    query: String,
    booths: List<Booth>,
    onQueryChange: (String) -> Unit,
    onSelectBooth: (Booth) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val results = remember(query, booths) { booths.matching(query) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = Dimens.ScreenPadding),
    ) {
        Spacer(Modifier.height(Dimens.Space))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onClose = onClose,
        )

        Spacer(Modifier.height(Dimens.Space))
        when {
            // The field opens empty. Showing "0 clubs" before a single character
            // has been typed reads as a failed search rather than a waiting one.
            query.isBlank() -> Spacer(Modifier.weight(1f))

            results.isEmpty() -> EmptyResults(
                query = query,
                modifier = Modifier.weight(1f),
            )

            else -> Results(
                results = results,
                onSelectBooth = onSelectBooth,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Case- and diacritic-insensitive substring match over name and blurb.
 *
 * Every word in the query has to match something, so "art club" narrows rather
 * than widening to everything containing "club" — which, in a list where every
 * entry is a club, would be the whole fair.
 */
internal fun List<Booth>.matching(query: String): List<Booth> {
    val terms = query.trim().lowercase().split(' ').filter(String::isNotEmpty)
    if (terms.isEmpty()) return emptyList()

    return filter { booth ->
        val haystack = "${booth.name} ${booth.about}".lowercase()
        terms.all { it in haystack }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    // Search that opens without a cursor in the box costs a tap for nothing —
    // the only reason to be on this screen is to type.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .glassSurface(cornerRadius = Dimens.RadiusPill)
                .padding(horizontal = Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = Ink.Faint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.booths_search_hint),
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Ink.Placeholder,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontFamily = AlanSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = Color.White,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search,
                    ),
                    cursorBrush = SolidColor(LocalAccent.current),
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = stringResource(R.string.booths_search_clear),
                        tint = Ink.Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Spacer(Modifier.size(Dimens.SpaceSm))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = stringResource(R.string.booths_search_close),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun Results(
    results: List<Booth>,
    onSelectBooth: (Booth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = pluralStringResource(
                R.plurals.booths_search_results,
                results.size,
                results.size,
            ),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
        Spacer(Modifier.height(Dimens.SpaceSm))
        LazyColumn(
            contentPadding = PaddingValues(bottom = Dimens.NavBarClearance),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            items(results, key = { it.number }) { booth ->
                ResultRow(booth = booth, onClick = { onSelectBooth(booth) })
            }
        }
    }
}

/**
 * One hit.
 *
 * Carries the zone, because the answer to "where is the Robotics Club" is a
 * place on the floor — a row that named the club and not the area it stands in
 * would answer the question the student did not need help with.
 */
@Composable
private fun ResultRow(
    booth: Booth,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusMd)
            .clip(RoundedCornerShape(Dimens.RadiusMd))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.Space)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Dimens.RadiusSm))
                .background(booth.zone.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(booth.icon),
                contentDescription = null,
                tint = booth.zone.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.size(Dimens.Space))
        Column(Modifier.weight(1f)) {
            Text(
                text = booth.name,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
            Text(
                text = stringResource(
                    R.string.booths_number_long,
                    booth.number,
                ) + "  ·  " + booth.zone.title,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Ink.Muted,
            )
        }
        if (booth.scanned) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.booths_scanned_desc),
                tint = booth.zone.accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Nothing matched.
 *
 * Names the query back, because on a phone the field may well be behind the
 * keyboard by the time this is read, and suggests the kind of word that does
 * work — the blurbs are indexed, which is not guessable from the outside.
 */
@Composable
private fun EmptyResults(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = Ink.Faint,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(Dimens.Space))
        Text(
            text = stringResource(R.string.booths_search_empty_title, query),
            modifier = Modifier.semantics { heading() },
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.booths_search_empty_body),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 1.45.em,
            color = Ink.Muted,
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun BoothSearchPreview() {
    SUClubFairTheme {
        Box {
            MeshBackground()
            BoothSearch(
                query = "art",
                booths = previewRoster(19),
                onQueryChange = {},
                onSelectBooth = {},
                onClose = {},
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun BoothSearchEmptyPreview() {
    SUClubFairTheme {
        Box {
            MeshBackground()
            BoothSearch(
                query = "quidditch",
                booths = previewRoster(19),
                onQueryChange = {},
                onSelectBooth = {},
                onClose = {},
            )
        }
    }
}
