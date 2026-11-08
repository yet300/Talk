package com.yet.talk.ui.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Copies [text]; a [sensitive] one (the private key) is flagged so the system's clipboard preview hides it. Android 13+
 * shows its own confirmation, so the toast is only for older versions.
 */
fun copyText(context: Context, text: String, confirmation: String, sensitive: Boolean = false) {
    val clip = ClipData.newPlainText(null, text)
    if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) toast(context, confirmation)
}

/** The design's toast ("Link copied", "Reported…"): the system's own. */
fun toast(context: Context, text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

/** The platform share sheet for a link or text. */
fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}

/** Whether screens with chats or the key are kept out of screenshots (Settings, Security); provided by the root. */
val LocalHideInScreenshots = staticCompositionLocalOf { true }

/**
 * Keeps the window out of screenshots, screen recordings and the recents preview while this is composed
 * (`FLAG_SECURE`), when [LocalHideInScreenshots] is on. Nested screens count, so the flag clears with the last one.
 * Inside a dialog the dialog's own window is secured too: it is a separate window that does not pick up a flag the
 * activity's window gets after the dialog opened.
 */
@Composable
fun SecureScreen() {
    val activity = LocalActivity.current ?: return
    val dialog = LocalView.current.dialogWindow()
    val enabled = LocalHideInScreenshots.current
    DisposableEffect(activity, dialog, enabled) {
        if (enabled) {
            if (secureScreens++ == 0) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            dialog?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (enabled && --secureScreens == 0) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

/** The window of the dialog this view is shown in, if it is in one. */
private fun View.dialogWindow(): Window? {
    var current: Any? = this
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = (current as? View)?.parent
    }
    return null
}

private var secureScreens = 0
