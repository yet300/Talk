package com.yet.talk.ui.screens.post

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.EmptyState
import com.yet.talk.ui.components.ErrorRow
import com.yet.talk.ui.components.LocalReaderMode
import com.yet.talk.ui.components.SkeletonCards
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassIconButton
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.screens.post.component.Article
import com.yet.talk.ui.screens.post.component.RepliesHeader
import com.yet.talk.ui.screens.post.component.ReplyCard
import com.yet.talk.ui.screens.post.dialog.DeletePostDialog
import com.yet.talk.ui.screens.post.sheet.MoreActionsSheet
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.stringResource

/**
 * The reader (`Reader · Mobile`): the article edge to edge under a glass bar with Back and More (a sheet, decision
 * 3), its title, author with Follow, actions, the quoted post, the markdown, tags and the post's address; then the
 * replies with a reply field. In two panes (Duo inner) the replies move to the left pane and Reply shows them.
 */
@Composable
fun PostContent(component: PostComponent) {
    val model by component.model.subscribeAsState()
    val reader = LocalReaderMode.current
    val replyFocus = remember { FocusRequester() }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    GlassPage(
        modifier = Modifier.imePadding(),
        bar = {
            BackBar(stringResource(MR.strings.post_title), onBack = component::onBack) {
                if (model.post != null) GlassIconButton(TalkIcons.More, stringResource(MR.strings.action_more), { sheet = true })
            }
        },
    ) { padding ->
        val post = model.post
        val shape = when {
            post != null -> ReaderState.Post
            model.isLoading -> ReaderState.Loading
            else -> ReaderState.Gone
        }
        AnimatedContent(shape, transitionSpec = { fadeIn(TalkMotion.fade()) togetherWith fadeOut(TalkMotion.fade()) }, label = "reader") { state ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                item(key = "error", contentType = ReaderItem.Error) { ErrorRow(model.error, onDismiss = component::onErrorShown) }
                when (state) {
                    ReaderState.Post -> if (post != null) {
                        item(key = "article", contentType = ReaderItem.Article) {
                            Article(post, model, component, onReply = { if (reader.twoPane) reader.onShowReplies() else replyFocus.requestFocus() })
                        }
                        if (!reader.twoPane) {
                            item(key = "replies", contentType = ReaderItem.RepliesHeader) { RepliesHeader(model, component, replyFocus) }
                            items(model.replies, key = { it.post.id.value }, contentType = { ReaderItem.Reply }) { reply ->
                                ReplyCard(reply, component, Modifier.animateItem())
                            }
                        }
                    }
                    ReaderState.Loading -> item(key = "loading", contentType = ReaderItem.State) { SkeletonCards(count = 1) }
                    ReaderState.Gone -> item(key = "gone", contentType = ReaderItem.State) { EmptyState(title = stringResource(MR.strings.post_unavailable)) }
                }
            }
        }
    }
    val post = model.post
    if (sheet && post != null) {
        MoreActionsSheet(post, model, component, onDelete = { confirmDelete = true }, onDismiss = { sheet = false })
    }
    if (confirmDelete) DeletePostDialog(onConfirm = { confirmDelete = false; component.onDelete() }, onDismiss = { confirmDelete = false })
}

/** What the reader shows: the post, its skeleton while it loads, or that it is gone. */
private enum class ReaderState { Post, Loading, Gone }

/** The reader's list item kinds, so Compose reuses each kind's layout. */
private enum class ReaderItem { Error, Article, RepliesHeader, Reply, State }
