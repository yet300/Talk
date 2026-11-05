package com.yet.talk.web.ui.screens.tab.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.yet.talk.feature.tab.search.SearchComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SearchField
import com.yet.talk.web.ui.components.list.PersonRow
import com.yet.talk.web.ui.components.list.SectionTitle
import com.yet.talk.web.ui.components.post.PostActionHandlers
import com.yet.talk.web.ui.components.post.PostCard
import com.yet.talk.web.ui.theme.stringResource
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Header
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

/** Search (`Search · Mobile`, results in the main column on the web): the field, then People and Posts. */
@Composable
fun SearchContent(component: SearchComponent) {
    val model by component.model.subscribeAsState()
    val actions = PostActionHandlers(component::onReply, component::onRepost, component::onLike, component::onBookmark, component::onOpenRevisions)

    Header({ classes("t-bar", "t-bar-sticky", "t-search-bar") }) {
        SearchField(model.query, component::onQueryChanged, stringResource(MR.strings.search_placeholder), id = "search")
    }
    // Opened from the right-column field: keep typing here.
    LaunchedEffect(Unit) { (document.getElementById("search") as? HTMLElement)?.focus() }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }

    when {
        model.isIdle -> P({ classes("t-caption", "t-search-hint") }) { Text(stringResource(MR.strings.search_idle)) }
        model.users.isEmpty() && model.posts.isEmpty() && !model.isSearching ->
            EmptyState(title = stringResource(MR.strings.search_empty, model.query.trim()))
    }
    if (model.isSearching) P({ classes("t-caption", "t-search-hint") }) { Text(stringResource(MR.strings.search_searching)) }
    if (model.users.isNotEmpty()) {
        SectionTitle(stringResource(MR.strings.search_people))
        model.users.forEach { user -> PersonRow(user, onClick = { component.onOpenProfile(user.id) }) }
    }
    if (model.posts.isNotEmpty()) {
        SectionTitle(stringResource(MR.strings.search_posts))
        model.posts.forEach { post ->
            PostCard(post, onOpen = { component.onOpenPost(it.id) }, onOpenAuthor = { component.onOpenProfile(it.author.id) }, actions = actions)
        }
    }
}
