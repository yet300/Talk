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
    const val RELAY_LIST = 10002
    const val BOOKMARKS = 10003
    const val BLOSSOM_AUTH = 24242
}
