package com.yet.talk.nostr.nip

import com.yet.talk.nostr.event.Event

/** NIP-92 media attachment. */
data class Imeta(
    val url: String,
    val mimeType: String? = null,
    val sha256: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val alt: String? = null,
) {
    fun toTag(): List<String> = listOfNotNull(
        "imeta",
        "url $url",
        mimeType?.let { "m $it" },
        sha256?.let { "x $it" },
        if (width != null && height != null) "dim ${width}x$height" else null,
        alt?.let { "alt $it" },
    )

    companion object {
        fun parse(tag: List<String>): Imeta? {
            if (tag.firstOrNull() != "imeta") return null
            val fields = tag.drop(1).mapNotNull { entry ->
                val space = entry.indexOf(' ')
                if (space <= 0) null else entry.substring(0, space) to entry.substring(space + 1)
            }.toMap()
            val url = fields["url"] ?: return null
            val dimensions = fields["dim"]?.split('x')
            return Imeta(
                url = url,
                mimeType = fields["m"],
                sha256 = fields["x"],
                width = dimensions?.getOrNull(0)?.toDoubleOrNull()?.toInt(),
                height = dimensions?.getOrNull(1)?.toDoubleOrNull()?.toInt(),
                alt = fields["alt"],
            )
        }
    }
}

fun Event.imeta(): List<Imeta> = tags.mapNotNull(Imeta::parse)
