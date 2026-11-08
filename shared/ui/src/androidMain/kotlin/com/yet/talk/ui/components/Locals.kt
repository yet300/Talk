package com.yet.talk.ui.components

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.yet.talk.domain.model.moderation.Moderation
import com.yet.talk.feature.post.component.cards.PostCardsComponent
import com.yet.talk.feature.root.moderation.ModerationComponent
import com.yet.talk.feature.post.component.cards.PreviewPostCardsComponent

/** The viewer's network, reported and blocked posts (decision 18); provided by the root content. */
val LocalModeration = compositionLocalOf { Moderation() }

/** Report (NIP-56); provided by the root content. */
val LocalModerationActions = staticCompositionLocalOf<ModerationComponent?> { null }

/** Quotes, link cards and Retry for every post card; provided by the root content. */
val LocalPostCards = staticCompositionLocalOf<PostCardsComponent> { PreviewPostCardsComponent() }

/** The post open next to a list (Duo inner, list | article): its card is highlighted (`t-selected`). */
val LocalSelectedPost = compositionLocalOf<com.yet.talk.domain.model.post.PostId?> { null }

/**
 * How the reader lays out: in two panes the replies take the left pane ([onShowReplies], decision 6) instead of
 * following the article.
 */
class ReaderMode(val twoPane: Boolean, val onShowReplies: () -> Unit)

val LocalReaderMode = staticCompositionLocalOf { ReaderMode(twoPane = false, onShowReplies = {}) }
