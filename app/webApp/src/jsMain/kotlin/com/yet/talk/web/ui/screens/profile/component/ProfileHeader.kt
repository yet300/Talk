package com.yet.talk.web.ui.screens.profile.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.domain.model.moderation.ReportTarget
import com.yet.talk.web.ui.components.IconButton
import com.yet.talk.web.ui.components.moderation.LocalModeration
import com.yet.talk.web.ui.components.moderation.ReportMenu
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
    // Outside your network the banner and avatar are blurred until Show (decision 18).
    var shown by remember(user?.id) { mutableStateOf(false) }
    val outside = user != null && !shown && !LocalModeration.current.isInNetwork(user.id)
    var reporting by remember { mutableStateOf(false) }
    Header({ classes("t-part") }) {
        Div({ classes("t-banner-img") }) {
            user?.banner?.let { Img(src = it, alt = "") { if (outside) classes("t-blur") } }
            if (outside && (user?.banner != null || user?.picture != null)) {
                val label = stringResource(MR.strings.moderation_show_profile)
                TalkButton(stringResource(MR.strings.action_show), { shown = true }, ButtonStyle.Secondary, attrs = {
                    classes("t-banner-show")
                    attr("aria-label", label)
                })
            }
        }
        Div({ classes("x-profile-top") }) {
            Avatar(user, AvatarSize.Profile, shown = shown)
            if (model.isMe) {
                TalkButton(stringResource(MR.strings.action_edit_profile), onEditProfile, ButtonStyle.Secondary, attrs = { classes("t-follow") })
            } else if (user != null) {
                Div({ classes("t-menu-host", "t-profile-more") }) {
                    IconButton(TalkIcons.More, stringResource(MR.strings.action_more), onClick = { reporting = !reporting }, attrs = {
                        attr("aria-haspopup", "menu")
                        attr("aria-expanded", reporting.toString())
                    })
                    if (reporting) ReportMenu(ReportTarget.Profile(user.id), onDismiss = { reporting = false })
                }
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
