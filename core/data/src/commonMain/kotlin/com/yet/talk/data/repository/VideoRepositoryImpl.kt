package com.yet.talk.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.yet.talk.api.schema.TalkEvent
import com.yet.talk.api.schema.TalkEvents
import com.yet.talk.api.schema.TalkSchema
import com.yet.talk.api.source.social.SocialRemoteSource
import com.yet.talk.data.video.catalog.VideoCatalogSource
import com.yet.talk.api.source.video.VideoRemoteSource
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.Publisher
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.SyncDao
import com.yet.talk.database.dao.VideoDao
import com.yet.talk.database.model.SyncStateRecord
import com.yet.talk.database.model.PublishStates
import com.yet.talk.database.model.VideoCommentRecord
import com.yet.talk.database.model.VideoLists
import com.yet.talk.database.model.VideoRecord
import com.yet.talk.domain.model.post.PublishState
import com.yet.talk.domain.model.user.User
import com.yet.talk.domain.model.user.UserId
import com.yet.talk.domain.model.video.Video
import com.yet.talk.domain.model.video.VideoComment
import com.yet.talk.domain.model.video.VideoFilters
import com.yet.talk.domain.model.video.VideoList
import com.yet.talk.domain.model.video.VideoStats
import com.yet.talk.domain.model.video.VideoViewer
import com.yet.talk.domain.repository.VideoRepository
import com.yet.talk.domain.repository.YouTubeRepository
import com.yet.talk.domain.rule.ContentRules
import com.yet.talk.domain.rule.FeedRules
import com.yet.talk.domain.rule.ProfileImageHosts
import com.yet.talk.nostr.encoding.Nip19
import com.yet.talk.nostr.nip.Kinds
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@SingleIn(AppScope::class)
@Inject
internal class VideoRepositoryImpl(
    private val videos: VideoDao,
    private val catalog: VideoCatalogSource,
    private val remoteSource: VideoRemoteSource,
    private val socialRemote: SocialRemoteSource,
    private val ingestor: EventIngestor,
    private val youtube: YouTubeRepository,
    private val publisher: Publisher,
    private val syncState: SyncDao,
) : VideoRepository {
    private val loading = Mutex()

    /**
     * Loads the bundled catalog into the database once per catalog version: an app with a refreshed catalog
     * loads it again (rows are replaced, lists and likes stay).
     */
    override suspend fun ensureCatalog(): Result<Unit> = remote {
        loading.withLock {
            if (syncState.state(CATALOG_KEY)?.cursor == catalog.version && videos.catalogSize() > 0) return@withLock
            videos.loadCatalog(catalog.catalogJson())
            syncState.setState(CATALOG_KEY, SyncStateRecord(endReached = true, cursor = catalog.version, syncedAt = publisher.now()))
        }
    }

    override fun categories(): Flow<List<String>> = videos.observeCategories()

    override fun browse(filters: VideoFilters): Flow<PagingData<Video>> = flow {
        val pages = videos.browsePages(filters.category, filters.level, filters.type)
        emitAll(Pager(PagingConfig(pageSize = FeedRules.PAGE_SIZE, enablePlaceholders = false), pagingSourceFactory = pages).flow)
    }.map { page -> page.map { it.toVideo() } }

    override fun forYou(limit: Int): Flow<List<Video>> = videos.observeForYou(limit).map { list -> list.map { it.toVideo() } }

    override fun list(list: VideoList): Flow<List<Video>> = videos.observeList(list.key).map { records -> records.map { it.toVideo() } }

    override fun observeVideo(id: String): Flow<Video?> = videos.observeVideo(id).flatMapLatest { record ->
        if (record != null) {
            flow { emit(record.toVideo(videos.categoriesOf(id))) }
        } else {
            youtube.video(id).map { it?.let { found -> Video(id, found.title, found.channel) } }
        }
    }

    override fun related(id: String, limit: Int): Flow<List<Video>> = videos.observeRelated(id, limit).map { list -> list.map { it.toVideo() } }

    override fun observeViewer(id: String): Flow<VideoViewer> =
        combine(videos.observeInList(id, VideoLists.LIKED), videos.observeInList(id, VideoLists.LATER)) { liked, later ->
            VideoViewer(liked = liked, watchLater = later)
        }

    override fun observeStats(id: String): Flow<VideoStats> =
        combine(videos.observeLikeCount(id), videos.observeComments(id)) { likes, comments -> VideoStats(likes, comments.size.toLong()) }

    override fun comments(id: String): Flow<List<VideoComment>> = videos.observeComments(id).mapLatest { list -> list.map { it.toComment() } }

    override suspend fun refresh(id: String): Result<Unit> = remote {
        val url = TalkSchema.videoUrl(id)
        val events = remoteSource.activity(url)
        events.forEach { event ->
            when (event) {
                is TalkEvent.UrlLike -> if (event.url == url) videos.insertLike(event.event.id, id, event.event.pubkey, event.event.createdAt)
                is TalkEvent.UrlComment -> if (event.url == url) {
                    videos.insertComment(event.event.id, id, event.event.pubkey, event.event.content, event.event.createdAt, PublishStates.PUBLISHED)
                }
                else -> Unit
            }
        }
        val authors = events.filterIsInstance<TalkEvent.UrlComment>().map { it.event.pubkey }.toSet()
        if (authors.isNotEmpty()) ingestor.ingest(socialRemote.profiles(authors))
    }

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = remote {
        val me = publisher.me()
        val existing = videos.likeIds(id, me)
        if (liked) {
            videos.setInList(id, VideoLists.LIKED, true, publisher.now())
            if (existing.isNotEmpty()) return@remote
            val event = publisher.sign { TalkEvents.videoLike(TalkSchema.videoUrl(id), it) }
            videos.insertLike(event.id, id, me, event.createdAt)
            try {
                publisher.publish(event)
            } catch (error: Throwable) {
                videos.deleteLikes(listOf(event.id), me) // the public like did not happen; the device list keeps it
                throw error
            }
        } else {
            // Deletions are published first: a tombstone cannot be taken back.
            if (existing.isNotEmpty()) publisher.publish(publisher.sign { TalkEvents.delete(existing.associateWith { Kinds.EXTERNAL_REACTION }, it) })
            videos.deleteLikes(existing, me)
            videos.setInList(id, VideoLists.LIKED, false, publisher.now())
        }
    }

    override suspend fun setWatchLater(id: String, on: Boolean) = videos.setInList(id, VideoLists.LATER, on, publisher.now())

    override suspend fun markWatched(id: String) = videos.setInList(id, VideoLists.HISTORY, true, publisher.now())

    override suspend fun comment(id: String, content: String): Result<Unit> {
        val text = ContentRules.validatePost(content, isReply = true).getOrElse { return Result.failure(it) }
        return remote {
            val event = publisher.sign { TalkEvents.videoComment(TalkSchema.videoUrl(id), text, it) }
            videos.insertComment(event.id, id, event.pubkey, event.content, event.createdAt, PublishStates.PENDING)
            val result = runCatching { publisher.publish(event) }
            videos.setCommentState(event.id, if (result.isSuccess) PublishStates.PUBLISHED else PublishStates.FAILED)
            result.getOrThrow()
        }
    }

    private val VideoList.key: String
        get() = when (this) {
            VideoList.Liked -> VideoLists.LIKED
            VideoList.WatchLater -> VideoLists.LATER
            VideoList.History -> VideoLists.HISTORY
        }
}

private fun VideoRecord.toVideo(categories: List<String> = emptyList()) = Video(
    id = vid, title = title, channel = channel, summary = summary, whyHn = whyHn, quote = quote, recommenders = recommenders,
    type = contentType, level = level, primaryTopic = primaryTopic, categories = categories,
)

private fun VideoCommentRecord.toComment() = VideoComment(
    id = id,
    author = User(
        UserId(author), Nip19.npub(author), authorName, authorDisplayName,
        picture = ProfileImageHosts.shownOrNull(authorPicture), nip05 = authorNip05,
    ),
    content = content,
    createdAt = Instant.fromEpochSeconds(createdAt),
    publishState = when (publishState) {
        PublishStates.PENDING -> PublishState.Pending
        PublishStates.FAILED -> PublishState.Failed
        else -> PublishState.Published
    },
)

/** The loaded catalog's version lives in sync_state under this key. */
private const val CATALOG_KEY = "video-catalog"
