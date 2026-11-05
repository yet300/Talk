package com.yet.talk.resources

import com.yet.talk.domain.model.user.User

/** The name shown for a user: display name, then name, then a shortened npub. */
val User.displayLabel: String
    get() = displayName?.takeIf(String::isNotBlank) ?: name?.takeIf(String::isNotBlank) ?: npub.shortNpub()

/** The secondary line: the NIP-05 address when there is one, else the shortened npub (`npub1w7r…4xze`). */
val User.handle: String
    get() = nip05?.takeIf(String::isNotBlank)?.removePrefix("_@") ?: npub.shortNpub()

/** One or two letters for the avatar placeholder. */
val User.initials: String
    get() = displayLabel.split(' ', '.', '_', '-').filter(String::isNotBlank).take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** `npub1w7r…4xze`: the prefix and four characters on each side. */
fun String.shortNpub(): String = if (length <= 16) this else take(8) + "…" + takeLast(4)
