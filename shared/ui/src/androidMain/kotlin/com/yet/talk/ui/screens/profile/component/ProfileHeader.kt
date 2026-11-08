package com.yet.talk.ui.screens.profile.component

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.domain.model.moderation.ReportReason
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.Avatar
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.LocalModeration
import com.yet.talk.ui.components.LocalModerationActions
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassSegments
import com.yet.talk.ui.components.glass.GlassTopBar
import com.yet.talk.ui.components.glass.LocalGlassSource
import com.yet.talk.ui.components.list.PagedList
import com.yet.talk.ui.components.post.PostActionHandlers
import com.yet.talk.ui.components.post.PostCard
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.message
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** The banner (or the surface), 134 dp avatar, Follow or Edit profile, name, `nip05 · npub`, about, links, counts. */
@Composable
fun ProfileHeader(model: ProfileComponent.Model, component: ProfileComponent) {
    val user = model.user
    val colors = MaterialTheme.talkColors
    val spacing = MaterialTheme.spacing
    val sizes = MaterialTheme.sizes
    // Outside your network the banner and avatar are blurred until Show (decision 18).
    var shown by rememberSaveable(user?.id?.value) { mutableStateOf(false) }
    val outside = user != null && !shown && !LocalModeration.current.isInNetwork(user.id)
    val bannerHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + sizes.bannerHeight * 0.66f
    Column {
        Box(Modifier.fillMaxWidth().height(bannerHeight).background(colors.surface)) {
            user?.banner?.takeIf(String::isNotBlank)?.let { banner ->
                AsyncImage(
                    model = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().then(if (outside) Modifier.blur(24.dp) else Modifier),
                )
            }
            if (outside && (user?.banner != null || user?.picture != null)) {
                val label = stringResource(MR.strings.moderation_show_profile)
                TalkButton(
                    stringResource(MR.strings.action_show), { shown = true }, ButtonStyle.Secondary,
                    modifier = Modifier.align(Alignment.Center).background(colors.bg, CircleShape).semantics { contentDescription = label },
                )
            }
        }
        // The avatar rises half out of this row, over the banner; the row itself is only as tall as what stays below.
        Row(Modifier.fillMaxWidth().height(AvatarSize / 2 + spacing.sm).padding(horizontal = spacing.lg), verticalAlignment = Alignment.Top) {
            Avatar(
                user,
                size = AvatarSize,
                shown = shown,
                modifier = Modifier.wrapContentHeight(Alignment.Top, unbounded = true).offset(y = -AvatarSize / 2).border(4.dp, colors.bg, CircleShape),
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(top = spacing.md)) {
                if (model.isMe) {
                    TalkButton(stringResource(MR.strings.action_edit_profile), component::onEditProfile, ButtonStyle.Secondary)
                } else if (user != null) {
                    com.yet.talk.ui.components.FollowButton(model.isFollowing, component::onFollowToggle)
                }
            }
        }
        Column(
            Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(user?.displayLabel.orEmpty(), style = MaterialTheme.talkTypography.display, color = colors.textPrimary)
            Text(
                listOfNotNull(user?.nip05?.takeIf(String::isNotBlank), user?.npub?.shortNpub()).joinToString(" · "),
                style = MaterialTheme.talkTypography.body,
                color = colors.textSecondary,
            )
            user?.about?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.talkTypography.body, color = colors.textPrimary) }
            if (user != null) ProfileLinks(user)
            Stats(model)
        }
    }
}

/** The website, and the GitHub account when its gist proof holds (NIP-39). */
@Composable
private fun ProfileLinks(user: User) {
    val uri = LocalUriHandler.current
    val colors = MaterialTheme.talkColors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        user.website?.takeIf(String::isNotBlank)?.let { site ->
            Row(
                Modifier.clickable(role = Role.Button) { uri.openUri(if (site.startsWith("http")) site else "https://$site") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TalkIcon(TalkIcons.Link, contentDescription = null, size = 18.dp, tint = colors.textSecondary)
                Text(site.removePrefix("https://").removePrefix("http://").removeSuffix("/"), style = MaterialTheme.talkTypography.body, color = colors.accent)
            }
        }
        user.github?.let { github ->
            val label = stringResource(MR.strings.github_badge, github.login)
            Row(
                Modifier.clickable(role = Role.Button) { uri.openUri("https://github.com/${github.login}") }.semantics { contentDescription = label },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TalkIcon(TalkIcons.GitHub, contentDescription = null, size = 18.dp, tint = colors.textSecondary)
                Text(github.login, style = MaterialTheme.talkTypography.body, color = colors.textPrimary)
                TalkIcon(TalkIcons.Check, contentDescription = null, size = 18.dp, tint = colors.accent)
            }
        }
    }
}

/** `posts · following · followers`. */
@Composable
private fun Stats(model: ProfileComponent.Model) {
    val colors = MaterialTheme.talkColors
    val parts = buildList {
        model.stats.posts?.let { add(it to stringResource(MR.strings.profile_articles)) }
        add(model.stats.following.toLong() to stringResource(MR.strings.profile_following))
        model.stats.followers?.let { add(it to stringResource(MR.strings.profile_followers)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)) {
        parts.forEach { (count, label) ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.textPrimary, fontWeight = MaterialTheme.talkTypography.bodyBold.fontWeight)) { append(count.toString()) }
                    append(" ")
                    withStyle(SpanStyle(color = colors.textSecondary)) { append(label) }
                },
                style = MaterialTheme.talkTypography.body,
            )
        }
    }
}


private val AvatarSize = 96.dp
