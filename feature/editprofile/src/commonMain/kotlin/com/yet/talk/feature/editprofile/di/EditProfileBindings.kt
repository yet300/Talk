package com.yet.talk.feature.editprofile.di

import com.yet.talk.feature.editprofile.DefaultEditProfileComponentFactory
import com.yet.talk.feature.editprofile.EditProfileComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class EditProfileBindings {
    @Binds
    internal abstract val DefaultEditProfileComponentFactory.bindEditProfileComponentFactory: EditProfileComponent.Factory
}
