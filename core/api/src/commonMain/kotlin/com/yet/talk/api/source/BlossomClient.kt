package com.yet.talk.api.source

import com.yet.talk.nostr.Event
import com.yet.talk.nostr.EventTemplate
import com.yet.talk.nostr.Signer
import com.yet.talk.nostr.nip.Kinds
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

/** A stored blob (BUD-02 blob descriptor). */
@Serializable
data class BlobDescriptor(
    val url: String,
    val sha256: String,
    val size: Long,
    val type: String? = null,
)

interface MediaRemoteSource {
    /** Uploads to Blossom, authorised by a kind 24242 event signed by [signer]. */
    suspend fun upload(bytes: ByteArray, mimeType: String, signer: Signer, createdAt: Long): BlobDescriptor
}

@ContributesBinding(AppScope::class)
@Inject
class BlossomClient(private val http: HttpClient) : MediaRemoteSource {

    override suspend fun upload(bytes: ByteArray, mimeType: String, signer: Signer, createdAt: Long): BlobDescriptor {
        val hash = SHA256().digest(bytes).toHexString()
        val authorization = signer.sign(
            EventTemplate(
                kind = Kinds.BLOSSOM_AUTH,
                content = "Upload image",
                tags = listOf(listOf("t", "upload"), listOf("x", hash), listOf("expiration", (createdAt + AUTH_TTL_SECONDS).toString())),
                createdAt = createdAt,
            ),
        )
        val response = http.put("$SERVER/upload") {
            header(HttpHeaders.Authorization, "Nostr " + Base64.encode(json.encodeToString(Event.serializer(), authorization).encodeToByteArray()))
            contentType(ContentType.parse(mimeType))
            setBody(bytes)
        }
        check(response.status.isSuccess()) { "Blossom upload failed: ${response.status}" }
        val blob = json.decodeFromString(BlobDescriptor.serializer(), response.bodyAsText())
        check(blob.sha256.equals(hash, ignoreCase = true)) { "Blossom returned a different blob" }
        return blob
    }

    private companion object {
        const val SERVER = "https://blossom.primal.net"
        const val AUTH_TTL_SECONDS = 300
        val json = Json { ignoreUnknownKeys = true }
    }
}
