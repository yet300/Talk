package com.yet.talk.data.repository

import com.yet.talk.domain.model.settings.ThemeMode
import com.yet.talk.domain.repository.ChannelRepository
import com.yet.talk.domain.repository.PreferencesRepository
import dev.zacsweers.metro.Inject
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Preferences in KSafe as plain (unencrypted) entries: nothing here is secret. */
@Inject
internal class PreferencesRepositoryImpl(private val ksafe: Lazy<KSafe>) : PreferencesRepository {

    override val theme: Flow<ThemeMode> = flow {
        emitAll(ksafe.value.getFlow(THEME, ThemeMode.System.name).map(::toTheme))
    }

    override suspend fun setTheme(mode: ThemeMode) = ksafe.value.put(THEME, mode.name, mode = KSafeWriteMode.Plain)

    override val channelNickname: Flow<String> = flow {
        emitAll(ksafe.value.getFlow(NICKNAME, ChannelRepository.DEFAULT_NICKNAME).map { it.ifBlank { ChannelRepository.DEFAULT_NICKNAME } })
    }

    override suspend fun setChannelNickname(nickname: String) {
        val clean = nickname.trim().filterNot { it.isISOControl() }.take(ChannelRepository.MAX_NICKNAME_LENGTH)
        ksafe.value.put(NICKNAME, clean.ifEmpty { ChannelRepository.DEFAULT_NICKNAME }, mode = KSafeWriteMode.Plain)
    }

    override val channelIntroSeen: Flow<Boolean> = flow { emitAll(ksafe.value.getFlow(INTRO_SEEN, false)) }

    override suspend fun setChannelIntroSeen() = ksafe.value.put(INTRO_SEEN, true, mode = KSafeWriteMode.Plain)

    private fun toTheme(name: String) = ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.System

    private companion object {
        const val THEME = "theme"
        const val NICKNAME = "channel_nickname"
        const val INTRO_SEEN = "channel_intro_seen"
    }
}
