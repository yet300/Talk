package com.yet.talk.feature.compose.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.media.ImageFile
import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.feature.compose.ComposeComponent
import com.yet.talk.feature.compose.ComposeMode

internal interface ComposeStore : Store<ComposeStore.Intent, ComposeStore.State, ComposeStore.Label> {

    data class State(
        val mode: ComposeMode,
        val text: String = "",
        val images: List<Image> = emptyList(),
        val target: Post? = null,
        /** An edit starts from the post's current content, once it is known. */
        val prefilled: Boolean = false,
        val view: ComposeComponent.View = ComposeComponent.View.Edit,
        val isUploading: Boolean = false,
        val isPublishing: Boolean = false,
        val error: DomainError? = null,
    )

    sealed interface Intent {
        data class ChangeText(val text: String) : Intent

        data class ChangeView(val view: ComposeComponent.View) : Intent

        data class AttachImage(val file: ImageFile) : Intent

        data class RemoveImage(val url: String) : Intent

        data object Publish : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object LoadTarget : Action
    }

    sealed interface Msg {
        data class TextChanged(val text: String) : Msg

        data class ViewChanged(val view: ComposeComponent.View) : Msg

        data class TargetLoaded(val post: Post) : Msg

        data class Uploading(val uploading: Boolean) : Msg

        data class ImageAdded(val image: Image) : Msg

        data class ImageRemoved(val url: String) : Msg

        data class Publishing(val publishing: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data class Published(val id: PostId) : Label
    }
}
