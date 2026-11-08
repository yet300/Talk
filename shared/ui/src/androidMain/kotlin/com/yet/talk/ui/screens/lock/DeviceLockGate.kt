package com.yet.talk.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yet.talk.resources.*
import com.yet.talk.ui.components.ButtonStyle
import com.yet.talk.ui.components.TalkButton
import com.yet.talk.ui.lock.DeviceLock
import com.yet.talk.ui.lock.LockSession
import com.yet.talk.ui.platform.SecureScreen
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.sizes
import com.yet.talk.ui.theme.spacing
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors
import com.yet.talk.ui.theme.talkTypography

/**
 * The app lock on Android (`Lock-Mobile`, decision 24): with the device lock on, Talk opens covered by "Talk is
 * locked" and asks for biometrics or the screen lock; leaving the app locks it again. [content] stays composed under
 * the cover (so nothing is lost) but cannot be seen, touched, read by TalkBack or captured in the recents preview.
 */
@Composable
fun DeviceLockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    // Without a screen lock there is nothing to unlock with: the prompt would fail every time and keep the person out
    // of Talk (and of the setting that turns the lock off), so the lock only applies while the device has one.
    var available by remember { mutableStateOf(DeviceLock.isAvailable(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { available = DeviceLock.isAvailable(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // A rotation recreates the activity, and Talk's own prompt or picker stop it: neither is leaving the app.
        if (activity?.isChangingConfigurations != true && !LockSession.awayOnPurpose) LockSession.unlocked = false
    }
    val locked = enabled && available && !LockSession.unlocked
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (locked) Modifier.clearAndSetSemantics {} else Modifier)) { content() }
        AnimatedVisibility(locked, enter = fadeIn(), exit = fadeOut()) { LockCover() }
    }
}

@Composable
private fun LockCover() {
    SecureScreen()
    val activity = LocalActivity.current as? FragmentActivity
    val title = stringResource(MR.strings.lock_title)
    val subtitle = stringResource(MR.strings.lock_device_body)
    val unlock = {
        if (activity != null && !LockSession.awayOnPurpose) {
            LockSession.awayOnPurpose = true
            DeviceLock.authenticate(activity, title, subtitle) { ok ->
                LockSession.awayOnPurpose = false
                if (ok) LockSession.unlocked = true
            }
        }
    }
    // Composed after the content, so this wins over its back handlers: back leaves Talk instead of navigating
    // behind the cover.
    BackHandler { activity?.moveTaskToBack(true) }
    // Ask at once; the button is there after a cancel.
    LaunchedEffect(Unit) { unlock() }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.talkColors.bg).pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .safeDrawingPadding().padding(MaterialTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg)) {
            Text(stringResource(MR.strings.app_name), style = MaterialTheme.talkTypography.wordmark, color = MaterialTheme.talkColors.textPrimary)
            TalkIcon(TalkIcons.Lock, contentDescription = null, size = MaterialTheme.sizes.iconEmpty, tint = MaterialTheme.talkColors.textSecondary)
            Text(title, style = MaterialTheme.talkTypography.headingLarge, color = MaterialTheme.talkColors.textPrimary)
            Text(subtitle, style = MaterialTheme.talkTypography.body, color = MaterialTheme.talkColors.textSecondary, textAlign = TextAlign.Center)
            TalkButton(stringResource(MR.strings.lock_unlock), unlock, ButtonStyle.Post, fullWidth = true)
        }
    }
}
