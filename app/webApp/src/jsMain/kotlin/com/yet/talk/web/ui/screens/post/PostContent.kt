package com.yet.talk.web.ui.screens.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.resources.*
import com.yet.talk.web.decompose.subscribeAsState
import com.yet.talk.web.ui.components.EmptyState
import com.yet.talk.web.ui.components.ErrorRow
import com.yet.talk.web.ui.components.SkeletonCards
import com.yet.talk.web.ui.components.TopBar
import com.yet.talk.web.ui.screens.post.component.Article
import com.yet.talk.web.ui.screens.post.component.MoreActions
import com.yet.talk.web.ui.screens.post.component.Replies
import com.yet.talk.web.ui.screens.post.dialog.DeletePostDialog
import com.yet.talk.web.ui.theme.stringResource
import org.jetbrains.compose.web.dom.Div

/**
 * The reader (`Reader · Web`): the article in the main column, its replies in the right column ([PostSide]).
 * Below 1018 px the right column hides and the replies follow the article.
 */
@Composable
fun PostContent(component: PostComponent) {
    val model by component.model.subscribeAsState()
    var confirmDelete by remember { mutableStateOf(false) }

    TopBar(stringResource(MR.strings.post_title), onBack = component::onBack) {
        model.post?.let { post -> MoreActions(post, model, component, onDelete = { confirmDelete = true }) }
    }
    model.error?.let { ErrorRow(it, onDismiss = component::onErrorShown) }
    val post = model.post
    when {
        post != null -> {
            Article(post, model, component)
            Div({ classes("t-narrow-only") }) { Replies(model, component, idSuffix = "inline") }
        }
        model.isLoading -> SkeletonCards(count = 1)
        else -> EmptyState(title = stringResource(MR.strings.post_unavailable))
    }
    if (confirmDelete) {
        DeletePostDialog(onConfirm = { confirmDelete = false; component.onDelete() }, onDismiss = { confirmDelete = false })
    }
}

/** The right column of the reader: the replies. */
@Composable
fun PostSide(component: PostComponent) {
    val model by component.model.subscribeAsState()
    if (model.post != null) Div({ classes("t-wide-only") }) { Replies(model, component, idSuffix = "side") }
}
