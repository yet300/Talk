package com.yet.talk.data.mapper

import com.yet.talk.database.model.ProfileRecord
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
    website = profile?.website,
    nip05 = profile?.nip05,
)
