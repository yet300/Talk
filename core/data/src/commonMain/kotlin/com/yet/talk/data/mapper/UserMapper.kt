package com.yet.talk.data.mapper

import com.yet.talk.database.model.ProfileRecord
import com.yet.talk.domain.model.github.GitHubAccount
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.nostr.encoding.Nip19

internal fun userOf(pubkey: String, profile: ProfileRecord?): User = User(
    id = UserId(pubkey),
    npub = Nip19.npub(pubkey),
    name = profile?.name,
    displayName = profile?.displayName,
    about = profile?.about,
    picture = ProfileImageHosts.shownOrNull(profile?.picture),
    banner = ProfileImageHosts.shownOrNull(profile?.banner),
    github = profile?.takeIf { it.githubVerified == true }?.let { GitHubAccount(it.githubLogin ?: return@let null, it.githubProof ?: return@let null) },
    website = profile?.website,
    nip05 = profile?.nip05,
)
