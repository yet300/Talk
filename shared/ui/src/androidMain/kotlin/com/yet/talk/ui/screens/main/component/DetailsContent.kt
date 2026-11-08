@file:OptIn(ExperimentalDecomposeApi::class)

package com.yet.talk.ui.screens.main.component

import com.yet.talk.ui.navigation.Page
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.yet.talk.feature.post.component.post.PostComponent
import com.yet.talk.feature.tab.main.component.details.DetailsComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.glass.BackBar
import com.yet.talk.ui.components.glass.GlassPage
import com.yet.talk.ui.navigation.pagesAnimation
import com.yet.talk.ui.screens.post.PostContent
import com.yet.talk.ui.screens.post.component.RepliesList
import com.yet.talk.ui.screens.profile.ProfileContent
import com.yet.talk.ui.screens.video.VideoContent
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The details panel (`ChildStack`): a post, and the authors, posts and videos opened from it, each sliding over the
 * last and going back with the predictive back gesture.
 */
@Composable
fun DetailsContent(details: DetailsComponent) {
    ChildStack(
        stack = details.stack,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg),
        animation = pagesAnimation(details.backHandler, details::onBack),
    ) { child -> Page {
        when (val page = child.instance) {
            is DetailsComponent.Child.Post -> PostContent(page.component)
            is DetailsComponent.Child.Profile -> ProfileContent(page.component, canGoBack = true)
            is DetailsComponent.Child.Video -> VideoContent(page.component)
        }
    }}
}

/** The right pane before a post is picked (Duo inner): the wordmark, faint. */
@Composable
fun DetailsPlaceholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg), contentAlignment = Alignment.Center) {
        Text(
            stringResource(MR.strings.app_name),
            style = MaterialTheme.talkTypography.wordmark,
            color = MaterialTheme.talkColors.textSecondary,
            modifier = Modifier.graphicsLayer { alpha = 0.4f },
        )
    }
}

/** The left pane in the replies pose (Duo inner: replies | article): the open post's replies, with Back to leave it. */
@Composable
fun ReadingPane(post: PostComponent) {
    GlassPage(bar = { BackBar(stringResource(MR.strings.profile_replies), onBack = null) }) { padding ->
        RepliesList(post, padding)
    }
}
