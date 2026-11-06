package com.yet.talk.feature.editprofile.mapper

import com.yet.talk.domain.rule.ProfileRules
import com.yet.talk.feature.editprofile.EditProfileComponent
import com.yet.talk.feature.editprofile.EditProfileComponent.Field
import com.yet.talk.feature.editprofile.store.EditProfileStore

internal val stateToModel: (EditProfileStore.State) -> EditProfileComponent.Model = { state ->
    val about = state.field(Field.About)
    val aboutLength = about.length - about.count { it.isLowSurrogate() }
    EditProfileComponent.Model(
        user = state.user,
        name = state.field(Field.Name),
        displayName = state.field(Field.DisplayName),
        about = about,
        aboutLength = aboutLength,
        aboutMax = ProfileRules.MAX_ABOUT_LENGTH,
        website = state.field(Field.Website),
        nip05 = state.field(Field.Nip05),
        nip05Status = state.nip05Status,
        avatar = state.avatar.url,
        banner = state.banner.url,
        isLoading = !state.isLoaded,
        isSaving = state.isSaving,
        canSave = state.isLoaded && !state.isSaving && aboutLength <= ProfileRules.MAX_ABOUT_LENGTH,
        error = state.error,
    )
}
