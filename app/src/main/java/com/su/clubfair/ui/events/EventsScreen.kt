package com.su.clubfair.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.su.clubfair.R
import com.su.clubfair.ui.components.Hairline
import com.su.clubfair.ui.components.glassSurface
import com.su.clubfair.ui.model.Announcement
import com.su.clubfair.ui.model.PlaceholderAnnouncements
import com.su.clubfair.ui.model.Reaction
import com.su.clubfair.ui.model.ReactionPalette
import com.su.clubfair.ui.scene.MeshBackground
import com.su.clubfair.ui.theme.AlanSans
import com.su.clubfair.ui.theme.Dimens
import com.su.clubfair.ui.theme.Ink
import com.su.clubfair.ui.theme.LocalAccent
import com.su.clubfair.ui.theme.Palette
import com.su.clubfair.ui.theme.SUClubFairTheme

/**
 * The Events tab: the Student Union's announcements channel, read-only.
 *
 * Modelled on a Discord announcement channel rather than a feed of cards, and
 * the difference is what it says about who is talking. A card grid reads as
 * content the app is showing you; a channel reads as a room with someone in it,
 * where posts arrive in order and the newest is the one at the bottom where you
 * are already looking.
 *
 * A student can't post — [isAdmin] decides whether there is a composer at the
 * bottom at all — but they aren't mute either: [ReactionPalette] gives them five
 * ways to answer, which is what keeps this from being a notice board nobody can
 * respond to.
 */
@Composable
fun EventsScreen(
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    announcements: List<Announcement> = PlaceholderAnnouncements,
) {
    // The feed is local and dies with the process. Reactions and anything posted
    // here have nowhere to go — there is no channel behind this, only the list.
    val feed = remember { mutableStateListOf<Announcement>().apply { addAll(announcements) } }
    val listState = rememberLazyListState()

    // Which post has its picker open, by id. One at a time: two open pickers on
    // screen would both look like the one the next tap belongs to.
    var pickerFor by rememberSaveable { mutableStateOf<Int?>(null) }

    // Land on the newest post, and follow the channel down as posts arrive — the
    // bottom is where a chat is read from.
    LaunchedEffect(feed.size) {
        if (feed.isNotEmpty()) listState.animateScrollToItem(feed.lastIndex)
    }

    // Opening a picker adds a row *below* the post, which on the newest one lands
    // under the footer — the list keeps its scroll position while the content
    // grows past it. Bring the post that owns the picker up to the top of the
    // viewport so its picker always has room underneath.
    LaunchedEffect(pickerFor) {
        val index = feed.indexOfFirst { it.id == pickerFor }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        ChannelHeader(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.SpaceLg,
                // A student has no composer under the list any more, so the last
                // post has to clear the floating nav bar by itself.
                bottom = if (isAdmin) Dimens.Space else Dimens.NavBarClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            itemsIndexed(feed, key = { _, post -> post.id }) { index, post ->
                AnnouncementRow(
                    post = post,
                    pickerOpen = pickerFor == post.id,
                    onTogglePicker = { pickerFor = if (pickerFor == post.id) null else post.id },
                    onReact = { emoji ->
                        feed[index] = post.toggling(emoji)
                        pickerFor = null
                    },
                )
            }
        }

        // Read out here: the send lambda runs on a tap, long after composition,
        // and a resource lookup is a composable call.
        val justNow = stringResource(R.string.events_just_now)

        // Nothing at all where the message box would be for a student. The
        // channel says "React, don't reply" at the top and every post ends in a
        // reaction row, so a locked composer explaining the same rule a third
        // time was just a permanent grey bar across the bottom of the screen.
        if (isAdmin) {
            Box(
                modifier = Modifier.padding(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = Dimens.NavBarClearance,
                ),
            ) {
                Composer(
                    onSend = { body ->
                        feed += Announcement(
                            id = (feed.maxOfOrNull { it.id } ?: 0) + 1,
                            author = "Student Union",
                            timestamp = justNow,
                            body = body,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Applies a tap on [emoji] to this post.
 *
 * Four cases, and the third is the one worth naming: reacting with an emoji that
 * already has other people on it joins their bucket rather than starting a
 * second one, so a chip's count is always the whole of that reaction.
 */
private fun Announcement.toggling(emoji: String): Announcement {
    val existing = reactions.firstOrNull { it.emoji == emoji }
    val updated = when {
        existing == null -> reactions + Reaction(emoji, count = 1, mine = true)
        // Last one out removes the chip, rather than leaving a dead 0.
        existing.mine && existing.count == 1 -> reactions - existing
        existing.mine -> reactions.map {
            if (it.emoji == emoji) it.copy(count = it.count - 1, mine = false) else it
        }
        else -> reactions.map {
            if (it.emoji == emoji) it.copy(count = it.count + 1, mine = true) else it
        }
    }
    return copy(reactions = updated)
}

@Composable
private fun ChannelHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = Dimens.Space)) {
        Text(
            text = stringResource(R.string.events_channel),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.events_channel_topic),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
        Spacer(Modifier.height(Dimens.Space))
        Hairline()
    }
}

@Composable
private fun AnnouncementRow(
    post: Announcement,
    pickerOpen: Boolean,
    onTogglePicker: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // No avatar and no role badge. Every post in this channel is by the same
    // author, so a repeated icon down the left margin and a repeated tag beside
    // the name were both stating the constant four times over — and the avatar
    // was costing the body a 52dp indent to do it. The name carries it.
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = post.author,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White,
            )
            Spacer(Modifier.size(Dimens.SpaceSm))
            Text(
                text = post.timestamp,
                fontFamily = AlanSans,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Ink.Faint,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceXs))
        Text(
            text = post.body,
            fontFamily = AlanSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 1.45.em,
            color = Ink.Label,
        )

        Spacer(Modifier.height(Dimens.SpaceSm))
        ReactionRow(
            reactions = post.reactions,
            pickerOpen = pickerOpen,
            onTogglePicker = onTogglePicker,
            onReact = onReact,
        )
    }
}

@Composable
private fun ReactionRow(
    reactions: List<Reaction>,
    pickerOpen: Boolean,
    onTogglePicker: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            reactions.forEach { reaction ->
                ReactionChip(
                    reaction = reaction,
                    onClick = { onReact(reaction.emoji) },
                )
            }
            AddReactionChip(open = pickerOpen, onClick = onTogglePicker)
        }

        // The picker sits under the chips rather than over them: a popup would
        // cover the post you are reacting to, and this row is five items wide.
        if (pickerOpen) {
            Spacer(Modifier.height(Dimens.SpaceXs))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.RadiusPill))
                    .glassSurface(cornerRadius = Dimens.RadiusPill)
                    .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                ReactionPalette.forEach { emoji ->
                    Text(
                        text = emoji,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onReact(emoji) }
                            .padding(Dimens.SpaceXs),
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionChip(
    reaction: Reaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            // Yours is lit and outlined, everyone else's is a plain grey chip —
            // the same tell Discord uses, and the only way to read your own
            // reaction back off a count you're already inside.
            .background(
                if (reaction.mine) accent.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (reaction.mine) accent.copy(alpha = 0.55f) else Color.Transparent,
                shape = RoundedCornerShape(Dimens.RadiusPill),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = reaction.emoji, fontSize = 13.sp)
        Spacer(Modifier.size(Dimens.SpaceXs))
        Text(
            text = "${reaction.count}",
            fontFamily = AlanSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = if (reaction.mine) accent else Ink.Muted,
        )
    }
}

@Composable
private fun AddReactionChip(
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.RadiusPill))
            .background(Color.White.copy(alpha = if (open) 0.16f else 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceSm, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.events_add_reaction),
            fontFamily = AlanSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Ink.Muted,
        )
    }
}

/** The Student Union's side of the channel — see [EventsScreen]'s `isAdmin`. */
@Composable
private fun Composer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val accent = LocalAccent.current
    val canSend = draft.isNotBlank()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = Dimens.RadiusPill)
            .padding(start = Dimens.CardPadding, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (draft.isEmpty()) {
                Text(
                    text = stringResource(R.string.events_composer_hint),
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Ink.Placeholder,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = TextStyle(
                    fontFamily = AlanSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Color.White,
                ),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.size(Dimens.SpaceSm))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (canSend) accent else Color.White.copy(alpha = 0.10f))
                .clickable(enabled = canSend) {
                    onSend(draft.trim())
                    draft = ""
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = stringResource(R.string.events_send),
                tint = if (canSend) Palette.Ink else Ink.Faint,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_7")
@Composable
private fun EventsPreview() {
    SUClubFairTheme {
        // The shell supplies the backdrop in the app; stand one in for the preview.
        Box {
            MeshBackground()
            EventsScreen()
        }
    }
}
