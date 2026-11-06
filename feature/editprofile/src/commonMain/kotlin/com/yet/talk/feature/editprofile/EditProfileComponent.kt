package com.yet.talk.feature.editprofile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.rule.ProfileImage

/** Edits the signed-in user's Nostr profile (kind 0). */
interface EditProfileComponent {
    val model: Value<Model>

    fun onFieldChanged(field: Field, text: String)

    /**
     * A pasted link for the avatar or banner. Only GIPHY and Unsplash links are accepted
     * ([com.yet.talk.domain.rule.ProfileImageHosts]); anything else sets [Model.error].
     */
    fun onImageLinkSubmitted(kind: ProfileImage, link: String)

    fun onGitHubLoginChanged(login: String)

    /** "Link GitHub": shows the proof text to put in a public gist. */
    fun onLinkGitHub()

    fun onGistLinkChanged(link: String)

    /** "Verify": checks the gist; saved with the profile. */
    fun onVerifyGitHub()

    /** Sets the avatar to the linked GitHub account's. */
    fun onUseGitHubAvatar()

    fun onSave()

    fun onCancel()

    fun onErrorShown()

    enum class Field { Name, DisplayName, About, Website, Nip05 }

    enum class Nip05Status { None, Checking, Verified, NotVerified }

    /** The GitHub link (`GitHubLink`): not linked, waiting for the gist (or failed), linked. */
    sealed interface GitHub {
        data class NotLinked(val login: String) : GitHub

        data class Waiting(val login: String, val proofText: String, val gistLink: String, val isChecking: Boolean, val failed: Boolean) : GitHub

        data class Linked(val login: String, val usesAvatar: Boolean) : GitHub
    }

    data class Model(
        /** For the avatar placeholder (initials) until a picture is set. */
        val user: User?,
        val name: String,
        val displayName: String,
        val about: String,
        /** Characters, as the limit counts them (an emoji is one). */
        val aboutLength: Int,
        val aboutMax: Int,
        val website: String,
        val nip05: String,
        val nip05Status: Nip05Status,
        val github: GitHub,
        /** Shown only from allowed sites; null shows the placeholder. */
        val avatar: String?,
        val banner: String?,
        /** True until the current profile has been loaded into the fields. */
        val isLoading: Boolean,
        val isSaving: Boolean,
        val canSave: Boolean,
        val error: DomainError?,
    )

    sealed interface Output {
        data object Closed : Output

        data object Saved : Output
    }

    fun interface Factory {
        operator fun invoke(componentContext: ComponentContext, output: (Output) -> Unit): EditProfileComponent
    }
}
