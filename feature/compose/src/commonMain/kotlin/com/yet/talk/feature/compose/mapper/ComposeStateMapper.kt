package com.yet.talk.feature.compose.mapper

import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode
import com.yet.talk.feature.compose.store.ComposeStore

internal val stateToModel: (ComposeStore.State) -> ComposeComponent.Model = { state ->
    val isReply = state.mode is ComposeMode.Reply || (state.mode is ComposeMode.Edit && state.target?.thread != null)
    ComposeComponent.Model(
        mode = state.mode,
        text = state.text,
        images = state.images,
        target = state.target,
        view = state.view,
        maxLength = if (isReply) ContentRules.MAX_REPLY_LENGTH else ContentRules.MAX_POST_LENGTH,
        isUploading = state.isUploading,
        isPublishing = state.isPublishing,
        canPublish = state.text.isNotBlank() && !state.isPublishing && !state.isUploading &&
            (state.mode !is ComposeMode.Edit || state.prefilled),
        error = state.error,
    )
}
