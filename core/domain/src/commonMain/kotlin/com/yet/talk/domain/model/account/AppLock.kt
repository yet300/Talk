package com.yet.talk.domain.model.account

/**
 * The app lock (Settings, Security). [passcodeSupported] where Talk locks with its own passcode (the web, NIP-49);
 * Android and iOS will use the device lock instead.
 */
data class AppLock(
    val passcodeSupported: Boolean = false,
    val enabled: Boolean = false,
    /** The key is still locked: the lock screen shows. */
    val locked: Boolean = false,
)
