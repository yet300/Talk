package com.yet.talk.nostr.nip

/** Event kinds from the NIP registry that Talk reads or writes. */
object Kinds {
    const val METADATA = 0
    const val TEXT_NOTE = 1
    const val CONTACTS = 3
    const val DELETION = 5
    const val REPOST = 6
    const val REACTION = 7
    const val GENERIC_REPOST = 16
    /** NIP-25 reaction to something outside Nostr, named by NIP-73 `i`/`k` tags. */
    const val EXTERNAL_REACTION = 17
    /** NIP-22 comment. */
    const val COMMENT = 1111
    /** NIP-56 report. */
    const val REPORT = 1984
    const val RELAY_LIST = 10002
    const val BOOKMARKS = 10003
    const val BLOSSOM_AUTH = 24242
}
