package com.yet.talk.domain.model.settings

/**
 * Device privacy settings (Settings, Security) where the platform has them: Android and iOS open Talk with the device
 * lock (biometrics or the device passcode) when [deviceLock] is on; Android hides chats and the key from screenshots
 * and the recents preview when [hideInScreenshots] is on (`FLAG_SECURE`).
 */
data class Privacy(
    val deviceLock: Boolean = false,
    val hideInScreenshots: Boolean = true,
)
