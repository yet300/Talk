package com.yet.talk.data.github

import com.yet.talk.api.source.github.GistDto
import com.yet.talk.api.source.github.GitHubRemoteSource
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.nip.githubProofText
import dev.zacsweers.metro.Inject

/** Checks NIP-39 GitHub claims: a public gist owned by the claimed login that contains the proof text for the key. */
@Inject
internal class GitHubProofs(private val github: GitHubRemoteSource) {
    /** The gist when it proves [pubkey] for [login], null when it does not; throws when GitHub cannot answer. */
    suspend fun check(pubkey: String, login: String, gistId: String): GistDto? {
        val gist = github.gist(gistId) ?: return null
        val proves = gist.ownerLogin.equals(login, ignoreCase = true) && githubProofText(Nip19.npub(pubkey)) in gist.text
        return gist.takeIf { proves }
    }
}
