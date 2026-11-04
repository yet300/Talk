package com.yet.talk.nostr.nip

/**
 * NIP-10 thread position of a kind 1 event: the thread [root], the direct [reply] parent, and
 * [mentions]. Marked `e` tags win; the deprecated positional form (first = root, last = reply) is
 * the fallback for older clients.
 */
data class ThreadReference(
    val root: String?,
    val reply: String?,
    val mentions: List<String>,
) {
    /** The event this one answers: the reply parent, or the root for a direct reply. */
    val parent: String? get() = reply ?: root

    val isReply: Boolean get() = parent != null

    companion object {
        fun parse(tags: List<List<String>>): ThreadReference {
            val eTags = tags.withName("e").filter { it.size >= 2 }
            val marked = eTags.filter { it.getOrNull(3) in MARKERS }
            if (marked.isNotEmpty()) {
                return ThreadReference(
                    root = marked.firstOrNull { it[3] == "root" }?.get(1),
                    reply = marked.firstOrNull { it[3] == "reply" }?.get(1),
                    mentions = eTags.filter { it.getOrNull(3) == "mention" }.map { it[1] },
                )
            }
            val ids = eTags.map { it[1] }
            return when (ids.size) {
                0 -> ThreadReference(null, null, emptyList())
                1 -> ThreadReference(root = ids[0], reply = null, mentions = emptyList())
                else -> ThreadReference(root = ids.first(), reply = ids.last(), mentions = ids.subList(1, ids.lastIndex))
            }
        }

        private val MARKERS = setOf("root", "reply", "mention")
    }
}
