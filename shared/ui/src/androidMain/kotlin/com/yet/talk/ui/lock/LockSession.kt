package com.yet.talk.ui.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Whether the device lock was passed in this run of the app; leaving the app clears it. */
object LockSession {
    var unlocked by mutableStateOf(false)

    /**
     * Talk itself sent the person to another screen and expects them back: the unlock prompt (and the screen-lock
     * activity it opens) or the photo picker. Stopping for that is not leaving the app, so it does not lock again.
     */
    var awayOnPurpose = false
}
