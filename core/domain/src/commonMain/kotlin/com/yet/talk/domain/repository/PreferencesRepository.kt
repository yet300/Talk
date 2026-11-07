package com.yet.talk.domain.repository

import com.yet.talk.domain.model.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device preferences: not account data, so signing out keeps them. */
interface PreferencesRepository {
    val theme: Flow<ThemeMode>

    suspend fun setTheme(mode: ThemeMode)

    /** The nickname shown in location channels ([ChannelRepository.DEFAULT_NICKNAME] until chosen). */
    val channelNickname: Flow<String>

    suspend fun setChannelNickname(nickname: String)

    /** Whether the location channels' first-run explainer was seen. */
    val channelIntroSeen: Flow<Boolean>

    suspend fun setChannelIntroSeen()
}
