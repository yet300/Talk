package com.yet.talk.database.dao

import androidx.paging.PagingSource
import com.yet.talk.database.model.VideoCommentRecord
import com.yet.talk.database.model.VideoRecord
import kotlinx.coroutines.flow.Flow

interface VideoDao {
    suspend fun catalogSize(): Long

    /** Loads the catalog JSON (an array of HackerTube videos, trimmed at build time) in one statement. */
    suspend fun loadCatalog(json: String)

    fun observeVideo(vid: String): Flow<VideoRecord?>

    suspend fun categoriesOf(vid: String): List<String>

    fun observeCategories(): Flow<List<String>>

    /** Null filters match everything. */
    suspend fun browsePages(category: String?, level: String?, type: String?): () -> PagingSource<Int, VideoRecord>

    fun observeList(list: String): Flow<List<VideoRecord>>

    fun observeInList(vid: String, list: String): Flow<Boolean>

    suspend fun setInList(vid: String, list: String, inList: Boolean, at: Long)

    fun observeForYou(limit: Int): Flow<List<VideoRecord>>

    fun observeRelated(vid: String, limit: Int): Flow<List<VideoRecord>>

    suspend fun insertLike(eventId: String, vid: String, user: String, createdAt: Long)

    suspend fun likeIds(vid: String, user: String): List<String>

    suspend fun deleteLikes(eventIds: List<String>, user: String)

    fun observeLikeCount(vid: String): Flow<Long>

    suspend fun insertComment(id: String, vid: String, author: String, content: String, createdAt: Long, publishState: String)

    suspend fun setCommentState(id: String, publishState: String)

    suspend fun deleteComments(ids: List<String>, author: String)

    fun observeComments(vid: String): Flow<List<VideoCommentRecord>>
}
