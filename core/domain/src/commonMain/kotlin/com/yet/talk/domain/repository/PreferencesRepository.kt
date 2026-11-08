package com.yet.talk.domain.repository

import com.yet.talk.domain.model.settings.Privacy
import com.yet.talk.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device preferences: not account data, so signing out keeps them. */
interface PreferencesRepository {
    val theme: Flow<ThemeMode>

    suspend fun setTheme(mode: ThemeMode)

    /** The device lock and hiding in screenshots (Android and iOS). */
    val privacy: Flow<Privacy>

    suspend fun setDeviceLock(enabled: Boolean)

    suspend fun setHideInScreenshots(hidden: Boolean)
}
