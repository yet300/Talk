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
    /** NIP-59 seal: the signed, encrypted rumor. */
    const val SEAL = 13
    /** NIP-17 chat message (a rumor, never published unwrapped). */
    const val CHAT_MESSAGE = 14
    /** NIP-59 gift wrap, signed by a throwaway key. */
    const val GIFT_WRAP = 1059
    /** NIP-17 relays to receive chats on. */
    const val DM_RELAYS = 10050
    /** NIP-42 relay authentication. */
    const val CLIENT_AUTH = 22242
    /** NIP-56 report. */
    const val REPORT = 1984
    const val RELAY_LIST = 10002
    const val BOOKMARKS = 10003
    const val BLOSSOM_AUTH = 24242
}
