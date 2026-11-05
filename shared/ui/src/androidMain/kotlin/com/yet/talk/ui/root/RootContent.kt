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
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/** The root stack with predictive back. Screens are placeholders until the UI phase replaces them. */
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
        when (child.instance) {
            RootComponent.Child.Splash -> Placeholder(stringResource(MR.strings.app_name))
            is RootComponent.Child.Auth -> Placeholder(stringResource(MR.strings.auth_title))
            is RootComponent.Child.Main -> Placeholder(stringResource(MR.strings.tab_home))
            is RootComponent.Child.Post -> Placeholder(stringResource(MR.strings.action_post))
            is RootComponent.Child.Revisions -> Placeholder(stringResource(MR.strings.revisions_title))
            is RootComponent.Child.Compose -> Placeholder(stringResource(MR.strings.compose_new))
            is RootComponent.Child.Profile -> Placeholder(stringResource(MR.strings.tab_profile))
            is RootComponent.Child.Settings -> Placeholder(stringResource(MR.strings.settings_title))
        }
    }
}

@Composable
private fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
    }
}
