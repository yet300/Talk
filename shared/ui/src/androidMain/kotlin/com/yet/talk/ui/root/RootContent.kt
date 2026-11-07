package com.yet.talk.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.screens.auth.AuthContent
import com.yet.talk.ui.screens.main.MainContent
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The root stack with predictive back; screens not built yet show their title. */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    Children(
        stack = component.stack,
        modifier = modifier.fillMaxSize().background(MaterialTheme.talkColors.bg),
        animation = predictiveBackAnimation(
            backHandler = component.backHandler,
            fallbackAnimation = stackAnimation(fade() + slide()),
            onBack = component::onBack,
        ),
    ) { child ->
        when (val instance = child.instance) {
            RootComponent.Child.Splash -> Placeholder(stringResource(MR.strings.app_name))
            is RootComponent.Child.Auth -> AuthContent(instance.component)
            // Android never locks with a passcode (the device lock comes later), so this is not reached.
            is RootComponent.Child.Lock -> Placeholder(stringResource(MR.strings.lock_title))
            is RootComponent.Child.Main -> MainContent(instance.component, onOpenSettings = component::onOpenSettings)
            is RootComponent.Child.Revisions -> Placeholder(stringResource(MR.strings.revisions_title))
            is RootComponent.Child.Compose -> Placeholder(stringResource(MR.strings.compose_new))
            is RootComponent.Child.Settings -> Placeholder(stringResource(MR.strings.settings_title))
            is RootComponent.Child.EditProfile -> Placeholder(stringResource(MR.strings.action_edit_profile))
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
    }
}
