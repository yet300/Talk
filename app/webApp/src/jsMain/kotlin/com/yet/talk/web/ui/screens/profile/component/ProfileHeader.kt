package com.yet.talk.web.ui.screens.profile.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The full profile header (`ProfileHeader`): banner ground, 134 px avatar, Follow (not on your own profile),
 * name, `nip05 · npub`, about, and `posts · following · followers`.
 */
@Composable
fun ProfileHeader(model: ProfileComponent.Model, onFollow: () -> Unit) {
    val user = model.user
    Header({ classes("t-part") }) {
        Div({ classes("x-banner") })
        Div({ classes("x-profile-top") }) {
            Avatar(user, AvatarSize.Profile)
            if (!model.isMe && user != null) {
                TalkButton(
                    text = stringResource(if (model.isFollowing) MR.strings.action_following else MR.strings.action_follow),
                    onClick = onFollow,
                    style = if (model.isFollowing) ButtonStyle.Secondary else ButtonStyle.Primary,
                    attrs = { classes("t-follow") },
                )
            }
        }
        Div({ classes("x-profile-info", "t-profile-info") }) {
            Div {
                H1({ classes("t-profile-name") }) { Text(user?.displayLabel.orEmpty()) }
                Div({ classes("x-handle") }) {
                    Text(listOfNotNull(user?.nip05?.takeIf(String::isNotBlank), user?.npub?.shortNpub()).joinToString(" · "))
                }
            }
            user?.about?.takeIf(String::isNotBlank)?.let { P({ classes("t-flush", "t-about") }) { Text(it) } }
            Div({ classes("t-stats") }) {
                model.stats.posts?.let { Stat(it, stringResource(MR.strings.profile_articles)) }
                Stat(model.stats.following.toLong(), stringResource(MR.strings.profile_following))
                model.stats.followers?.let { Stat(it, stringResource(MR.strings.profile_followers)) }
            }
        }
    }
}

@Composable
private fun Stat(value: Long, label: String) {
    Span {
        Span({ classes("t-strong") }) { Text(value.toString()) }
        Text(" ")
        Span({ classes("t-muted") }) { Text(label) }
    }
}
