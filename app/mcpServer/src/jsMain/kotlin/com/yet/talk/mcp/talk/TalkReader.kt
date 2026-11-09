package com.yet.talk.mcp.talk

import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.rule.TrendingRanker
import com.yet.talk.mcp.agent.Disclosure
import com.yet.talk.mcp.agent.ToolError
import com.yet.talk.mcp.di.TalkSources
import com.yet.talk.nostr.encoding.Nip19
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal enum class FeedKind { Latest, Following, Trending }

internal enum class SearchKind { Posts, Profiles }

/** Reads Talk from relays as the apps show it: the newest revision of each post, deleted posts left out. */
internal class TalkReader(private val sources: TalkSources, private val me: String, private val now: () -> Long) {

    suspend fun feed(kind: FeedKind, until: Long?, limit: Int): JsonObject {
        return when (kind) {
            FeedKind.Latest -> page(topLevel(sources.posts.timeline(null, until, limit * OVERFETCH), limit))
            FeedKind.Following -> {
                val following = sources.social.lists(me).filterIsInstance<TalkEvent.Contacts>().firstOrNull()?.pubkeys.orEmpty()
                page(topLevel(sources.posts.timeline(following, until, limit * OVERFETCH), limit))
            }
            // A ranking has no time cursor to continue from.
            FeedKind.Trending -> page(trending(limit), hasCursor = false)
        }
    }

    suspend fun post(id: String): JsonObject {
        val post = sources.posts.byIds(listOf(id)).firstOrNull() ?: throw ToolError("No Talk post with that id on these relays")
        val attached = attachments(listOf(post))
        if (id in attached.deleted) throw ToolError("That post was deleted by its author")
        val replies = sources.posts.thread(post.thread.root ?: id).filter { it.event.id != id }.sortedBy { it.event.createdAt }
        val threadAttached = attachments(replies)
        val authors = authors(listOf(post) + replies)
        return buildJsonObject {
            put("post", postJson(post, attached, authors))
            put("replies", JsonArray(replies.filter { it.event.id !in threadAttached.deleted }.map { postJson(it, threadAttached, authors) }))
        }
    }

    suspend fun profile(pubkey: String, until: Long?, limit: Int): JsonObject {
        val profile = newestProfiles(listOf(pubkey))[pubkey]
        val posts = topLevel(sources.posts.timeline(listOf(pubkey), until, limit * OVERFETCH), limit)
        return buildJsonObject {
            put("profile", profileJson(pubkey, profile))
            put("followers", sources.social.countFollowers(pubkey)?.let(::JsonPrimitive) ?: JsonNull)
            put("posts", page(posts))
        }
    }

    suspend fun search(query: String, kind: SearchKind, limit: Int): JsonObject = when (kind) {
        SearchKind.Posts -> page(sources.search.searchPosts(query, limit).filterNot { it.thread.isReply }.take(limit))
        SearchKind.Profiles -> buildJsonObject {
            put("profiles", JsonArray(sources.search.searchProfiles(query, limit).map { profileJson(it.event.pubkey, it) }))
        }
    }

    suspend fun activity(until: Long?, limit: Int): JsonObject {
        val events = sources.social.activity(me, until, limit).sortedByDescending { it.event.createdAt }.take(limit)
        return buildJsonObject {
            putJsonArray("activity") {
                events.mapNotNull(::activityJson).forEach { add(it) }
            }
            events.lastOrNull()?.let { put("next_until", it.event.createdAt - 1) }
        }
    }

    private fun activityJson(event: TalkEvent): JsonObject? = when (event) {
        is TalkEvent.Post -> buildJsonObject {
            put("type", if (event.thread.isReply) "reply" else "quote")
            put("post_id", event.thread.parent ?: event.quoteId)
            put("id", event.event.id)
            put("markdown", event.event.content)
            common(event)
        }
        is TalkEvent.Reaction -> buildJsonObject {
            put("type", if (event.type == TalkEvent.Reaction.Type.Like) "like" else "repost")
            put("post_id", event.postId)
            common(event)
        }
        else -> null
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.common(event: TalkEvent) {
        put("actor", event.event.pubkey)
        put("created_at", event.event.createdAt)
    }

    private fun topLevel(posts: List<TalkEvent.Post>, limit: Int) = posts.filterNot { it.thread.isReply }.take(limit)

    private suspend fun trending(limit: Int): List<TalkEvent.Post> {
        val time = now()
        val since = time - TrendingRanker.WINDOW.inWholeSeconds
        val candidates = topLevel(sources.posts.since(since, TRENDING_FETCH), TRENDING_FETCH).associateBy { it.event.id }
        val related = sources.posts.related(candidates.keys)
        val reactions = sources.posts.standingReactions(related).map { reaction ->
            val type = if (reaction.type == TalkEvent.Reaction.Type.Like) TrendingRanker.Engagement.Type.Like else TrendingRanker.Engagement.Type.Repost
            TrendingRanker.Engagement(PostId(reaction.postId), UserId(reaction.event.pubkey), type)
        }
        val replies = related.filterIsInstance<TalkEvent.Post>().mapNotNull { reply ->
            reply.thread.parent?.let { TrendingRanker.Engagement(PostId(it), UserId(reply.event.pubkey), TrendingRanker.Engagement.Type.Reply) }
        }
        val engagements = reactions + replies
        val ranked = TrendingRanker.rank(
            candidates.values.map { TrendingRanker.Candidate(PostId(it.event.id), UserId(it.event.pubkey), Instant.fromEpochSeconds(it.event.createdAt)) },
            engagements,
            Instant.fromEpochSeconds(time),
        )
        return ranked.take(limit).mapNotNull { candidates[it.postId.value] }
    }

    private suspend fun page(posts: List<TalkEvent.Post>, hasCursor: Boolean = true): JsonObject {
        val attached = attachments(posts)
        val visible = posts.filter { it.event.id !in attached.deleted }
        val authors = authors(visible)
        return buildJsonObject {
            put("posts", JsonArray(visible.map { postJson(it, attached, authors) }))
            if (hasCursor) posts.lastOrNull()?.let { put("next_until", it.event.createdAt - 1) }
        }
    }

    private suspend fun authors(posts: List<TalkEvent.Post>) = newestProfiles(posts.map { it.event.pubkey }.distinct())

    private suspend fun newestProfiles(pubkeys: List<String>): Map<String, TalkEvent.Profile> =
        sources.social.profiles(pubkeys).groupBy { it.event.pubkey }.mapValues { (_, versions) -> versions.maxBy { it.event.createdAt } }

    private suspend fun attachments(posts: List<TalkEvent.Post>): Attachments {
        val related = sources.posts.related(posts.map { it.event.id })
        val authorOf = posts.associate { it.event.id to it.event.pubkey }
        val revisions = related.filterIsInstance<TalkEvent.Revision>().filter { authorOf[it.postId] == it.event.pubkey }.groupBy { it.postId }
            .mapValues { (_, list) -> list.maxWith(compareBy({ it.number }, { it.event.createdAt })) }
        val deleted = related.filterIsInstance<TalkEvent.Deletion>().flatMap { deletion -> deletion.eventIds.filter { authorOf[it] == deletion.event.pubkey } }.toSet()
        val reactions = sources.posts.standingReactions(related)
        fun count(type: TalkEvent.Reaction.Type) = reactions.filter { it.type == type }.distinctBy { it.postId to it.event.pubkey }.groupingBy { it.postId }.eachCount()
        val replies = related.filterIsInstance<TalkEvent.Post>().mapNotNull { it.thread.parent }.groupingBy { it }.eachCount()
        return Attachments(revisions, deleted, count(TalkEvent.Reaction.Type.Like), count(TalkEvent.Reaction.Type.Repost), replies)
    }

    private class Attachments(
        val revisions: Map<String, TalkEvent.Revision>,
        val deleted: Set<String>,
        val likes: Map<String, Int>,
        val reposts: Map<String, Int>,
        val replies: Map<String, Int>,
    )

    private fun postJson(post: TalkEvent.Post, attached: Attachments, authors: Map<String, TalkEvent.Profile>): JsonObject {
        val id = post.event.id
        val revision = attached.revisions[id]
        return buildJsonObject {
            put("id", id)
            put("note", Nip19.note(id))
            put("author", profileJson(post.event.pubkey, authors[post.event.pubkey]))
            put("created_at", post.event.createdAt)
            put("markdown", revision?.event?.content ?: post.event.content)
            put("revision", revision?.number ?: 1)
            post.thread.parent?.let { put("reply_to", it) }
            post.thread.root?.let { put("thread_root", it) }
            post.quoteId?.let { put("quotes", it) }
            put("likes", attached.likes[id] ?: 0)
            put("reposts", attached.reposts[id] ?: 0)
            put("replies", attached.replies[id] ?: 0)
        }
    }

    private fun profileJson(pubkey: String, profile: TalkEvent.Profile?): JsonElement = buildJsonObject {
        put("pubkey", pubkey)
        put("npub", Nip19.npub(pubkey))
        profile?.metadata?.name?.let { put("name", it) }
        profile?.metadata?.displayName?.let { put("display_name", it) }
        profile?.metadata?.about?.let { put("about", it) }
        val raw = profile?.let { runCatching { Json.parseToJsonElement(it.event.content).jsonObject }.getOrNull() }
        put("bot", raw != null && Disclosure.isBot(raw))
    }

    private companion object {
        /** Feeds hold replies too, and those are dropped here. */
        const val OVERFETCH = 3
        const val TRENDING_FETCH = 500
    }
}
