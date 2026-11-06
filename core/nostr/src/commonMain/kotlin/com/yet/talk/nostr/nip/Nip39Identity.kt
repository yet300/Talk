package com.yet.talk.nostr.nip

/** A NIP-39 external identity claim on a kind 0: `["i", "<platform>:<identity>", "<proof>"]`. */
data class ExternalIdentity(val platform: String, val identity: String, val proof: String)

fun List<List<String>>.externalIdentities(): List<ExternalIdentity> = withName("i").mapNotNull { tag ->
    val claim = tag.getOrNull(1) ?: return@mapNotNull null
    val proof = tag.getOrNull(2) ?: return@mapNotNull null
    val platform = claim.substringBefore(':', "").takeIf(String::isNotEmpty) ?: return@mapNotNull null
    ExternalIdentity(platform, claim.substringAfter(':'), proof)
}

fun ExternalIdentity.toTag(): List<String> = listOf("i", "$platform:$identity", proof)

/** [tags] without the claims for [platform], plus [identity] when given. Other tags keep their order. */
fun replaceIdentity(tags: List<List<String>>, platform: String, identity: ExternalIdentity?): List<List<String>> =
    tags.filterNot { it.firstOrNull() == "i" && it.getOrNull(1)?.startsWith("$platform:") == true } + listOfNotNull(identity?.toTag())

/** NIP-39's GitHub proof: a public gist by the user containing this text. */
fun githubProofText(npub: String): String = "Verifying that I control the following Nostr public key: $npub"
