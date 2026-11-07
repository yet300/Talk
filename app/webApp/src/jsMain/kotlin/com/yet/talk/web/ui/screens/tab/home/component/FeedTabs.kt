package com.yet.talk.web.ui.screens.tab.home.component

import androidx.compose.runtime.Composable
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.resources.*
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.TalkButton
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import com.yet.talk.web.ui.components.Tabs

/** Following · Popular · Latest; Popular adds the caption about how it is ranked. */
@Composable
fun FeedTabs(selected: HomeComponent.Feed, onSelect: (HomeComponent.Feed) -> Unit) {
    Tabs(
        items = HomeComponent.Feed.entries,
        selected = selected,
        label = { it.label() },
        onSelect = onSelect,
        ariaLabel = stringResource(MR.strings.tab_home),
        // A floating glass capsule under the bar (design pass 4).
        capsule = true,
    )
    if (selected == HomeComponent.Feed.Popular) {
        P({ classes("t-caption", "t-popular-caption") }) { Text(stringResource(MR.strings.feed_popular_caption)) }
    }
}

/** The empty feed; Following points to Latest ("Browse Latest"), as in the design. */
@Composable
fun FeedEmpty(feed: HomeComponent.Feed, onBrowseLatest: () -> Unit) {
    if (feed == HomeComponent.Feed.Following) {
        EmptyState(
            title = stringResource(MR.strings.feed_following_empty_title),
            body = stringResource(MR.strings.feed_following_empty_body),
            action = { TalkButton(stringResource(MR.strings.action_browse_latest), onBrowseLatest) },
        )
    } else {
        EmptyState(title = stringResource(MR.strings.feed_empty_title), body = stringResource(MR.strings.feed_empty_body))
    }
}

@Composable
private fun HomeComponent.Feed.label(): String = stringResource(
    when (this) {
        HomeComponent.Feed.Following -> MR.strings.feed_following
        HomeComponent.Feed.Popular -> MR.strings.feed_popular
        HomeComponent.Feed.Latest -> MR.strings.feed_latest
    },
)
