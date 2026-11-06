package com.yet.talk.feature.editprofile.store

import com.arkivanov.mvikotlin.core.store.Store
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.feature.editprofile.EditProfileComponent.Nip05Status

internal interface EditProfileStore : Store<EditProfileStore.Intent, EditProfileStore.State, EditProfileStore.Label> {

    /** An avatar or banner; only a [changed] one is saved, so an image Talk does not show is never dropped. */
    data class Slot(val url: String? = null, val changed: Boolean = false)

    data class State(
        val user: User? = null,
        val fields: Map<Field, String> = emptyMap(),
        /** Once the user types or picks an image, a late profile load no longer replaces the fields. */
        val isEdited: Boolean = false,
        val isLoaded: Boolean = false,
        val avatar: Slot = Slot(),
        val banner: Slot = Slot(),
        val nip05Status: Nip05Status = Nip05Status.None,
        val isSaving: Boolean = false,
        val error: DomainError? = null,
    ) {
        fun field(field: Field): String = fields[field].orEmpty()

        fun slot(kind: ProfileImage): Slot = if (kind == ProfileImage.Avatar) avatar else banner
    }

    sealed interface Intent {
        data class ChangeField(val field: Field, val text: String) : Intent

        data class SubmitImageLink(val kind: ProfileImage, val link: String) : Intent

        data object Save : Intent

        data object DismissError : Intent
    }

    sealed interface Action {
        data object Load : Action
    }

    sealed interface Msg {
        data class UserLoaded(val user: User) : Msg

        /** Relays answered (or failed): the fields can be edited. */
        data object Loaded : Msg

        data class FieldChanged(val field: Field, val text: String) : Msg

        data class SlotChanged(val kind: ProfileImage, val slot: Slot) : Msg

        data class Nip05Checked(val status: Nip05Status) : Msg

        data class Saving(val saving: Boolean) : Msg

        data class Failed(val error: DomainError?) : Msg
    }

    sealed interface Label {
        data object Saved : Label
    }
}
