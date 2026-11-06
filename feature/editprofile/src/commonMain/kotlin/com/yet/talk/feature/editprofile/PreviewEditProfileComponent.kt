package com.yet.talk.feature.editprofile

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.yet.talk.domain.rule.ProfileImage
import com.yet.talk.domain.rule.ProfileRules

/** For UI previews: a fixed model, actions do nothing. */
class PreviewEditProfileComponent(
    model: EditProfileComponent.Model = EditProfileComponent.Model(
        user = null, name = "yara", displayName = "Yara Nadim", about = "Building small clients for big networks.", aboutLength = 40,
        aboutMax = ProfileRules.MAX_ABOUT_LENGTH, website = "nadim.dev", nip05 = "yara@nadim.dev",
        nip05Status = EditProfileComponent.Nip05Status.Verified, avatar = null, banner = null,
        isLoading = false, isSaving = false, canSave = true, error = null,
    ),
) : EditProfileComponent {
    override val model: Value<EditProfileComponent.Model> = MutableValue(model)

    override fun onFieldChanged(field: EditProfileComponent.Field, text: String) = Unit

    override fun onImageLinkSubmitted(kind: ProfileImage, link: String) = Unit

    override fun onSave() = Unit

    override fun onCancel() = Unit

    override fun onErrorShown() = Unit
}
