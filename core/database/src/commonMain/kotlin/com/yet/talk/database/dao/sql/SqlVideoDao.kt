package com.yet.talk.database.dao.sql

import androidx.paging.PagingSource
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.app.common.AppDispatchers
import com.yet.talk.database.DatabaseHolder
import com.yet.talk.database.dao.VideoDao
import com.yet.talk.database.model.VideoCommentRecord
import com.yet.talk.database.model.VideoRecord
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
internal class SqlVideoDao(private val holder: DatabaseHolder, private val dispatchers: AppDispatchers) : VideoDao {

    override suspend fun catalogSize(): Long = withContext(dispatchers.io) { holder.get().videoQueries.countVideos().awaitAsOne() }

    /**
     * One statement per table, so the web worker gets two messages instead of ~20,000. The dialect check of
     * `.sq` files does not know json_each, so these run on the driver directly.
     */
    override suspend fun loadCatalog(json: String) = withContext(dispatchers.io) {
        val database = holder.get()
        val driver = holder.driver()
        database.transaction {
            driver.execute(null, INSERT_VIDEOS, 1) { bindString(0, json) }.await()
            driver.execute(null, INSERT_CATEGORIES, 1) { bindString(0, json) }.await()
        }
        driver.notifyListeners("video", "video_category")
    }

    override fun observeVideo(vid: String): Flow<VideoRecord?> =
        holder.observeOneOrNull(dispatchers.io) { videoQueries.selectVideo(vid, ::videoRecord) }

    override suspend fun categoriesOf(vid: String): List<String> =
        withContext(dispatchers.io) { holder.get().videoQueries.selectCategoriesOf(vid).awaitAsList() }

    override fun observeCategories(): Flow<List<String>> = holder.observeList(dispatchers.io) { videoQueries.selectCategories() }

    override suspend fun browsePages(category: String?, level: String?, type: String?): () -> PagingSource<Int, VideoRecord> {
        val queries = holder.get().videoQueries
        val count = queries.countBrowse(category, level, type)
        return {
            OffsetPagingSource(count, dispatchers.io) { limit: Long, offset: Long ->
                queries.selectBrowse(category, level, type, limit, offset, ::videoRecord)
            }
        }
    }

    override fun observeList(list: String): Flow<List<VideoRecord>> =
        holder.observeList(dispatchers.io) { videoQueries.selectList(list, ::videoRecord) }

    override fun observeInList(vid: String, list: String): Flow<Boolean> =
        holder.observeOne(dispatchers.io) { videoQueries.isInList(vid, list) }

    override suspend fun setInList(vid: String, list: String, inList: Boolean, at: Long) = withContext(dispatchers.io) {
        val queries = holder.get().videoQueries
        if (inList) queries.addToList(vid, list, at) else queries.removeFromList(vid, list)
        Unit
    }

    override fun observeForYou(limit: Int): Flow<List<VideoRecord>> =
        holder.observeList(dispatchers.io) { videoQueries.selectForYou(limit.toLong(), ::videoRecord) }

    override fun observeRelated(vid: String, limit: Int): Flow<List<VideoRecord>> =
        holder.observeList(dispatchers.io) { videoQueries.selectRelated(vid, limit.toLong(), ::videoRecord) }

    override suspend fun insertLike(eventId: String, vid: String, user: String, createdAt: Long) = withContext(dispatchers.io) {
        holder.get().videoQueries.insertLike(eventId, vid, user, createdAt)
        Unit
    }

    override suspend fun likeIds(vid: String, user: String): List<String> =
        withContext(dispatchers.io) { holder.get().videoQueries.selectLikeIds(vid, user).awaitAsList() }

    override suspend fun deleteLikes(eventIds: List<String>, user: String) = withContext(dispatchers.io) {
        if (eventIds.isNotEmpty()) holder.get().videoQueries.deleteLikes(eventIds, user)
        Unit
    }

    override fun observeLikeCount(vid: String): Flow<Long> = holder.observeOne(dispatchers.io) { videoQueries.countLikes(vid) }

    override suspend fun insertComment(id: String, vid: String, author: String, content: String, createdAt: Long, publishState: String) =
        withContext(dispatchers.io) {
            holder.get().videoQueries.insertComment(id, vid, author, content, createdAt, publishState)
            Unit
        }

    override suspend fun setCommentState(id: String, publishState: String) = withContext(dispatchers.io) {
        holder.get().videoQueries.setCommentState(publishState, id)
        Unit
    }

    override suspend fun deleteComments(ids: List<String>, author: String) = withContext(dispatchers.io) {
        if (ids.isNotEmpty()) holder.get().videoQueries.deleteComments(ids, author)
        Unit
    }

    override fun observeComments(vid: String): Flow<List<VideoCommentRecord>> =
        holder.observeList(dispatchers.io) {
            videoQueries.selectComments(vid) { id, video, author, content, createdAt, state, name, displayName, picture, nip05 ->
                VideoCommentRecord(id, video, author, content, createdAt, state, name, displayName, picture, nip05)
            }
        }

    private companion object {
        const val INSERT_VIDEOS =
            "INSERT OR REPLACE INTO video(vid, title, channel, summary, why_hn, content_type, level, primary_topic, quote, recommenders, score) " +
                "SELECT json_extract(value, '\$.vid'), json_extract(value, '\$.title'), COALESCE(json_extract(value, '\$.channel'), ''), " +
                "json_extract(value, '\$.summary'), json_extract(value, '\$.why_hn'), json_extract(value, '\$.content_type'), " +
                "json_extract(value, '\$.level'), json_extract(value, '\$.primary_topic'), json_extract(value, '\$.quote'), " +
                "COALESCE(json_extract(value, '\$.recommenders'), 0), COALESCE(json_extract(value, '\$.score'), 0) " +
                "FROM json_each(?) WHERE json_extract(value, '\$.vid') IS NOT NULL AND json_extract(value, '\$.title') IS NOT NULL"
        const val INSERT_CATEGORIES =
            "INSERT OR IGNORE INTO video_category(vid, category) " +
                "SELECT json_extract(v.value, '\$.vid'), c.value FROM json_each(?) v, json_each(v.value, '\$.categories') c " +
                "WHERE json_extract(v.value, '\$.vid') IS NOT NULL AND c.type = 'text'"
    }
}

@Suppress("LongParameterList")
private fun videoRecord(
    vid: String, title: String, channel: String, summary: String?, whyHn: String?, contentType: String?, level: String?,
    primaryTopic: String?, quote: String?, recommenders: Long, score: Double,
) = VideoRecord(vid, title, channel, summary, whyHn, contentType, level, primaryTopic, quote, recommenders, score)
