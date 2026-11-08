@file:OptIn(ExperimentalDecomposeApi::class)

package com.yet.talk.ui.root

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.widthIn
import com.yet.talk.ui.navigation.Page
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.stack.ChildStack
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.yet.talk.feature.root.RootComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.components.LocalModeration
import com.yet.talk.ui.components.LocalModerationActions
import com.yet.talk.ui.components.LocalNetwork
import com.yet.talk.ui.components.LocalPostCards
import com.yet.talk.ui.navigation.pagesAnimation
import com.yet.talk.ui.platform.LocalHideInScreenshots
import com.yet.talk.ui.screens.auth.AuthContent
import com.yet.talk.ui.screens.compose.ComposeContent
import com.yet.talk.ui.screens.editprofile.EditProfileContent
import com.yet.talk.ui.screens.lock.DeviceLockGate
import com.yet.talk.ui.screens.lock.LockContent
import com.yet.talk.ui.screens.main.MainContent
import com.yet.talk.ui.screens.revisions.RevisionsContent
import com.yet.talk.ui.screens.settings.SettingsContent
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The root stack (`ChildStack`): Splash, Auth, Lock and Main swap with a crossfade; the editor, Settings, Revisions
 * and Edit profile slide over Main and go back with the predictive back gesture. The device lock covers everything
 * once signed in, and every screen reads the network, moderation and post-card locals provided here.
 */
@Composable
fun RootContent(component: RootComponent, modifier: Modifier = Modifier) {
    val moderation by component.moderation.model.subscribeAsState()
    val network by component.network.subscribeAsState()
    val privacy by component.privacy.subscribeAsState()
    val stack by component.stack.subscribeAsState()
    val signedIn = stack.items.any { it.instance is RootComponent.Child.Main }
    CompositionLocalProvider(
        LocalModeration provides moderation,
        LocalModerationActions provides component.moderation,
        LocalPostCards provides component.postCards,
        LocalNetwork provides network,
        LocalHideInScreenshots provides privacy.hideInScreenshots,
    ) {
        DeviceLockGate(enabled = privacy.deviceLock && signedIn) {
            ChildStack(
                stack = component.stack,
                modifier = modifier.fillMaxSize().background(MaterialTheme.talkColors.bg),
                animation = pagesAnimation(
                    backHandler = component.backHandler,
                    onBack = component::onBack,
                    swap = { child, other -> child.instance.isTopLevel && other.instance.isTopLevel },
                ),
            ) { child -> Page {
                when (val instance = child.instance) {
                    RootComponent.Child.Splash -> Splash()
                    is RootComponent.Child.Auth -> AuthContent(instance.component)
                    // Talk's own passcode is the web's (NIP-49); Android locks with the device instead, so this shows
                    // only for an account carried over with a passcode.
                    is RootComponent.Child.Lock -> LockContent(instance.component)
                    is RootComponent.Child.Main -> MainContent(instance.component, onOpenSettings = component::onOpenSettings)
                    is RootComponent.Child.Revisions -> Readable { RevisionsContent(instance.component) }
                    is RootComponent.Child.Compose -> ComposeContent(instance.component)
                    is RootComponent.Child.Settings -> Readable { SettingsContent(instance.component) }
                    is RootComponent.Child.EditProfile -> Readable { EditProfileContent(instance.component) }
                }
            }}
        }
    }
}

/** Destinations that replace each other rather than stack. */
private val RootComponent.Child.isTopLevel: Boolean
    get() = this is RootComponent.Child.Splash || this is RootComponent.Child.Auth || this is RootComponent.Child.Lock || this is RootComponent.Child.Main

/** Until the saved key has been read: the wordmark on the page background. */
@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(MR.strings.app_name), style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
    }
}

/**
 * A form or list screen on a large window (the fold open, a tablet): a centred column of a readable width instead of
 * rows stretched across the whole screen, on the page background.
 */
@Composable
private fun Readable(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ReadableWidth).fillMaxSize()) { content() }
    }
}

private val ReadableWidth = 680.dp
