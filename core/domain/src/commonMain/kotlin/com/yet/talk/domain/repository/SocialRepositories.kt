package com.yet.talk.domain.repository

import com.yet.talk.domain.model.ActivityItem
import com.yet.talk.domain.model.ImageFile
import com.yet.talk.domain.model.MediaUpload
import com.yet.talk.domain.model.Pager
import com.yet.talk.domain.model.Post
import com.yet.talk.domain.model.PostId
import com.yet.talk.domain.model.ProfileEdit
import com.yet.talk.domain.model.Relay
import com.yet.talk.domain.model.User
import com.yet.talk.domain.model.UserId
import com.yet.talk.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** A placeholder user (id and npub only) until the profile is known. */
    fun observeUser(id: UserId): Flow<User>

    fun observeStats(id: UserId): Flow<UserStats>

    suspend fun refresh(id: UserId): Result<Unit>

    suspend fun updateMyProfile(edit: ProfileEdit): Result<Unit>
}

/** Ranked by [com.yet.talk.domain.rule.TrendingRanker]; the cursor is the rank. */
interface TrendingRepository {
    fun trending(): Pager<Post>
}

interface ReactionRepository {
    suspend fun setLiked(postId: PostId, liked: Boolean): Result<Unit>

    suspend fun setReposted(postId: PostId, reposted: Boolean): Result<Unit>
}

interface BookmarkRepository {
    suspend fun setBookmarked(postId: PostId, bookmarked: Boolean): Result<Unit>

    fun bookmarks(): Pager<Post>
}

interface FollowRepository {
    fun observeIsFollowing(id: UserId): Flow<Boolean>

    suspend fun setFollowing(id: UserId, following: Boolean): Result<Unit>
}

interface ActivityRepository {
    /** Replies, quotes, reposts and likes of the viewer's posts, and new followers, newest first. */
    fun activity(): Pager<ActivityItem>
}

interface MediaRepository {
    suspend fun upload(file: ImageFile): Result<MediaUpload>
}

interface RelayRepository {
    val relays: Flow<List<Relay>>

    suspend fun setRelays(relays: List<Relay>): Result<Unit>
}
