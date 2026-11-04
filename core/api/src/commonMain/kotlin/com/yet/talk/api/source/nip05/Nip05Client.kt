package com.yet.talk.api.source.nip05

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@ContributesBinding(AppScope::class)
@Inject
class Nip05Client(private val http: HttpClient) : Nip05Verifier {

    override suspend fun verify(identifier: String, pubkey: String): Boolean = runCatching {
        val (name, domain) = identifier.lowercase().split('@').takeIf { it.size == 2 } ?: return false
        if (domain.isBlank() || '/' in domain) return false
        val response = http.get("https://$domain/.well-known/nostr.json?name=$name")
        if (!response.status.isSuccess()) return false
        val names = Json.parseToJsonElement(response.bodyAsText()).jsonObject["names"]?.jsonObject ?: return false
        names[name]?.jsonPrimitive?.content.equals(pubkey, ignoreCase = true)
    }.getOrDefault(false)
}
