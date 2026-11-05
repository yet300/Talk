package com.yet.talk.data.repository

import com.yet.talk.api.source.search.SearchRemoteSource
import com.yet.talk.data.mapper.toPost
import com.yet.talk.data.mapper.userOf
import com.yet.talk.data.session.Session
import com.yet.talk.data.sync.EventIngestor
import com.yet.talk.data.sync.RemoteSync
import com.yet.talk.data.sync.remote
import com.yet.talk.database.dao.PostDao
import com.yet.talk.database.dao.ProfileDao
import com.yet.talk.domain.model.search.SearchResults
import com.yet.talk.domain.repository.SearchRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
@Inject
internal class SearchRepositoryImpl(
    private val posts: PostDao,
    private val profiles: ProfileDao,
    private val searchRemote: SearchRemoteSource,
    private val sync: RemoteSync,
    private val ingestor: EventIngestor,
    private val session: Session,
) : SearchRepository {

    override fun observeResults(query: String): Flow<SearchResults> {
        val text = query.trim()
        if (text.length < SearchRepository.MIN_QUERY_LENGTH) return flowOf(SearchResults())
        return session.viewer.flatMapLatest { viewer ->
            combine(profiles.observeSearch(text, USER_LIMIT), posts.observeSearch(text, viewer, POST_LIMIT)) { users, found ->
                SearchResults(users = users.map { userOf(it.pubkey, it) }, posts = found.map { it.toPost() })
            }
        }
    }

    override suspend fun search(query: String): Result<Unit> = remote {
        val text = query.trim()
        if (text.length < SearchRepository.MIN_QUERY_LENGTH) return@remote
        sync.storeWithContext(searchRemote.searchPosts(text, POST_LIMIT))
        ingestor.ingest(searchRemote.searchProfiles(text, USER_LIMIT))
    }

    private companion object {
        const val USER_LIMIT = 20
        const val POST_LIMIT = 50
    }
}
