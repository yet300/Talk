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

/** Read helpers over raw tags. */
fun List<List<String>>.withName(name: String): List<List<String>> = filter { it.firstOrNull() == name }

/** The second element of every tag called [name] (`["e", id, …]` → ids). */
fun List<List<String>>.values(name: String): List<String> = withName(name).mapNotNull { it.getOrNull(1) }

fun List<List<String>>.firstValue(name: String): String? = values(name).firstOrNull()

/** Tag builders. Empty optional parts are dropped from the end, as most clients do. */
object Tags {
    fun event(id: String, relay: String = "", marker: String? = null, author: String? = null): List<String> =
        listOfNotNull("e", id, relay, marker ?: author?.let { "" }, author).trimEmptyEnd()

    fun pubkey(pubkey: String, relay: String = ""): List<String> = listOf("p", pubkey, relay).trimEmptyEnd()

    fun quote(id: String, relay: String = "", author: String? = null): List<String> =
        listOfNotNull("q", id, relay, author).trimEmptyEnd()

    fun kind(kind: Int): List<String> = listOf("k", kind.toString())

    fun hashtag(tag: String): List<String> = listOf("t", tag.lowercase())

    private fun List<String>.trimEmptyEnd(): List<String> = dropLastWhile { it.isEmpty() }
}
