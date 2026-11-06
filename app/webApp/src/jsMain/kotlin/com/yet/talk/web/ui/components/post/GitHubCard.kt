package com.yet.talk.web.ui.components.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.feature.post.component.cards.GitHubCardsComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.theme.TalkIcon
import com.yet.talk.web.ui.theme.TalkIcons
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/** Loads GitHub cards; provided by the root content. */
val LocalGitHubCards = staticCompositionLocalOf<GitHubCardsComponent?> { null }

/**
 * A GitHub link in a post (`RepoCard`): the repository (description, language, stars, forks) or the user
 * (name, bio, followers). While it loads, or when GitHub has none, the plain link row (`LinkRow`).
 */
@Composable
fun GitHubCardView(ref: GitHubRef) {
    val cards = LocalGitHubCards.current
    val card by remember(ref, cards) { cards?.card(ref) ?: kotlinx.coroutines.flow.flowOf(null) }.collectAsState(null)
    when (val loaded = card) {
        null -> LinkRow(ref.url)
        is GitHubCard.Repo -> CardLink(ref.url, loaded.avatarUrl, loaded.fullName) {
            Div({ classes("x-name", "t-ellipsis") }) { Text(loaded.fullName) }
            loaded.description?.takeIf(String::isNotBlank)?.let { Div({ classes("t-clamp2") }) { Text(it) } }
            Div({ classes("t-meta-row") }) {
                loaded.language?.let { Span({ classes("t-inline") }) { Span({ classes("t-dot") }); Text(it) } }
                Span({ classes("t-inline") }) { TalkIcon(TalkIcons.Star, size = 18f); Text(count(loaded.stars)) }
                Span({ classes("t-inline") }) { TalkIcon(TalkIcons.Fork, size = 18f); Text(count(loaded.forks)) }
            }
        }
        is GitHubCard.User -> CardLink(ref.url, loaded.avatarUrl, loaded.ref.login) {
            Div({ classes("x-name", "t-ellipsis") }) { Text(loaded.name ?: loaded.ref.login) }
            loaded.bio?.takeIf(String::isNotBlank)?.let { Div({ classes("t-clamp2") }) { Text(it) } }
            Div({ classes("t-meta-row") }) { Span { Text(stringResource(MR.strings.github_followers, count(loaded.followers))) } }
        }
    }
}

/** A link that is not a rich card: the globe, the domain and the address (`LinkRow`). */
@Composable
fun LinkRow(url: String) {
    val address = url.removePrefix("https://").removePrefix("http://").removePrefix("www.")
    A(href = url, attrs = { external() ; classes("t-part", "t-linkrow") }) {
        TalkIcon(TalkIcons.Globe, size = 18f)
        Span({ classes("t-grow") }) {
            Span({ classes("x-name", "t-ellipsis", "t-block") }) { Text(address.substringBefore('/')) }
            Span({ classes("t-caption", "t-ellipsis", "t-block") }) { Text(address) }
        }
    }
}

@Composable
private fun CardLink(url: String, avatarUrl: String?, initialsOf: String, content: @Composable () -> Unit) {
    A(href = url, attrs = { external(); classes("t-repo") }) {
        Span({ classes("x-avatar"); attr("aria-hidden", "true") }) {
            if (avatarUrl != null) Img(src = avatarUrl, alt = "") { classes("t-avatar-img") } else Text(initialsOf.take(2).uppercase())
        }
        Div({ classes("t-grow", "t-repo-text") }) { content() }
    }
}

/** Opens in a new tab and does not open the post the card sits in. */
private fun org.jetbrains.compose.web.attributes.AttrsScope<org.w3c.dom.HTMLAnchorElement>.external() {
    target(ATarget.Blank)
    attr("rel", "noopener noreferrer nofollow")
    onClick { it.stopPropagation() }
}

/** 1204 → "1,204", in the reader's locale. */
private fun count(value: Long): String = value.asDynamic().toLocaleString().unsafeCast<String>()
