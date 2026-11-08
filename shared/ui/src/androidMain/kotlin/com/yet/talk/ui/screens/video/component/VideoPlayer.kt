package com.yet.talk.ui.screens.video.component

import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.yet.talk.feature.video.VideoComponent
import com.yet.talk.resources.*
import com.yet.talk.ui.theme.TalkIcon
import com.yet.talk.ui.theme.TalkIcons
import com.yet.talk.ui.theme.TalkMotion
import com.yet.talk.ui.theme.stringResource
import com.yet.talk.ui.theme.talkColors

/**
 * The player (`VideoPlayer`): the thumbnail with Play, then YouTube's own player in its privacy-enhanced mode
 * (`youtube-nocookie.com`, decision 15), crossfading from the thumbnail. The catalog's videos are YouTube videos, so
 * the IFrame player is the supported way to play them (Media3 would need the raw streams, which YouTube does not
 * offer). Its fullscreen button takes the whole screen, sideways, until back or the button again.
 */
@Composable
fun VideoPlayer(model: VideoComponent.Model, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9).background(Color.Black)) {
        Crossfade(model.isPlaying, animationSpec = TalkMotion.fade(), label = "player") { playing ->
            if (playing) {
                YouTubeEmbed(model.id)
            } else {
                val label = stringResource(MR.strings.video_play_video)
                Box(
                    Modifier.fillMaxSize().clickable(role = Role.Button, onClick = onPlay).semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage("https://i.ytimg.com/vi/${model.id}/hqdefault.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.talkColors.mediaScrim), contentAlignment = Alignment.Center) {
                        TalkIcon(TalkIcons.Play, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

/** The fullscreen player view the library hands over, and how to leave it. */
private class Fullscreen(val view: View, val exit: () -> Unit)

@Composable
private fun YouTubeEmbed(id: String) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var fullscreen by remember { mutableStateOf<Fullscreen?>(null) }
    val player = remember(id) {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            addFullscreenListener(
                object : FullscreenListener {
                    override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                        fullscreen = Fullscreen(fullscreenView, exitFullscreen)
                    }

                    override fun onExitFullscreen() {
                        fullscreen = null
                    }
                },
            )
            val options = IFramePlayerOptions.Builder(context.applicationContext)
                .controls(1)
                .fullscreen(1)
                .rel(0)
                .ivLoadPolicy(3)
                .privacyEnhancedMode(true)
                .build()
            initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) = youTubePlayer.loadVideo(id, 0f)
                },
                options,
            )
        }
    }
    // Pauses with the screen and frees the WebView when the page goes.
    DisposableEffect(player, lifecycle) {
        lifecycle.addObserver(player)
        onDispose {
            lifecycle.removeObserver(player)
            player.release()
        }
    }
    AndroidView(factory = { player }, modifier = Modifier.fillMaxSize())
    fullscreen?.let { FullscreenPlayer(it) }
}

/**
 * The player over everything, edge to edge and sideways, with the system bars hidden; back leaves it. The window
 * returns to the orientation it had before.
 */
@Composable
private fun FullscreenPlayer(fullscreen: Fullscreen) {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (activity != null && previous != null) activity.requestedOrientation = previous }
    }
    Dialog(
        onDismissRequest = fullscreen.exit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {}
        }
        BackHandler(onBack = fullscreen.exit)
        AndroidView(
            factory = { fullscreen.view.also { (it.parent as? ViewGroup)?.removeView(it) } },
            modifier = Modifier.fillMaxSize().background(Color.Black),
            onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        )
    }
}

