package com.yet.talk.web.ui.screens.profile.component

import androidx.compose.runtime.Composable
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.Avatar
import com.yet.talk.web.ui.components.AvatarSize
import com.yet.talk.web.ui.components.ButtonStyle
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * The full profile header (`ProfileHeader`): the banner (or the surface colour), 134 px avatar, Follow on
 * someone else's profile or Edit profile on yours, name, `nip05 · npub`, about, the website and the verified
 * GitHub account, and `posts · following · followers`.
 */
@Composable
fun ProfileHeader(model: ProfileComponent.Model, onFollow: () -> Unit, onEditProfile: () -> Unit) {
    val user = model.user
    Header({ classes("t-part") }) {
        Div({ classes("t-banner-img") }) { user?.banner?.let { Img(src = it, alt = "") } }
        Div({ classes("x-profile-top") }) {
            Avatar(user, AvatarSize.Profile)
            if (model.isMe) {
                TalkButton(stringResource(MR.strings.action_edit_profile), onEditProfile, ButtonStyle.Secondary, attrs = { classes("t-follow") })
            } else if (user != null) {
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
            if (user != null && (user.website?.isNotBlank() == true || user.github != null)) ProfileLinks(user)
            Div({ classes("t-stats") }) {
                model.stats.posts?.let { Stat(it, stringResource(MR.strings.profile_articles)) }
                Stat(model.stats.following.toLong(), stringResource(MR.strings.profile_following))
                model.stats.followers?.let { Stat(it, stringResource(MR.strings.profile_followers)) }
            }
        }
    }
}

/** The website, and the GitHub account when its gist proof holds (a link to github.com/<login>). */
@Composable
private fun ProfileLinks(user: User) {
    Div({ classes("t-profile-links") }) {
        user.website?.takeIf(String::isNotBlank)?.let { site ->
            A(href = if (site.startsWith("http")) site else "https://$site", attrs = {
                classes("x-link")
                attr("target", "_blank")
                attr("rel", "noopener noreferrer nofollow")
            }) { Text(site.removePrefix("https://").removePrefix("http://").removeSuffix("/")) }
        }
        user.github?.let { github ->
            val label = stringResource(MR.strings.github_badge, github.login)
            A(href = "https://github.com/${github.login}", attrs = {
                classes("t-gh")
                attr("target", "_blank")
                attr("rel", "noopener noreferrer")
                attr("aria-label", label)
            }) {
                TalkIcon(TalkIcons.GitHub, size = 18f)
                Text(github.login)
                TalkIcon(TalkIcons.Check, size = 18f)
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
