package com.yet.talk.feature.post.component.cards

import com.yet.talk.domain.model.github.GitHubCard
import com.yet.talk.domain.model.github.GitHubRef
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.model.post.YouTubeVideo
import com.yet.talk.domain.repository.GitHubRepository
import com.yet.talk.domain.repository.PostRepository
import com.yet.talk.domain.repository.YouTubeRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * What every post card needs beyond the post (`PostCard`), on every screen that lists posts: the GitHub and
 * YouTube cards for its link, the quoted post, and Retry on Not sent. Exposed by the root component.
 */
interface PostCardsComponent {
    /** Null while loading or when GitHub has none: the link row shows. */
    fun gitHub(ref: GitHubRef): Flow<GitHubCard?>

    /** Title and channel; null while loading or when YouTube has none (the thumbnail still shows). */
    fun youTube(videoId: String): Flow<YouTubeVideo?>

    /** The quoted post, fetched from relays when it is not stored yet. */
    fun quote(id: PostId): Flow<Post?>

    /** "Retry" on a post of yours that is Not sent. */
    fun retry(post: Post)

    /** A video card on the phone opens the video page (the web plays it in the card). */
    fun openVideo(id: String)
}

/** For UI previews: no cards, so link rows show. */
class PreviewPostCardsComponent : PostCardsComponent {
    override fun gitHub(ref: GitHubRef): Flow<GitHubCard?> = flowOf(null)

    override fun youTube(videoId: String): Flow<YouTubeVideo?> = flowOf(null)

    override fun quote(id: PostId): Flow<Post?> = flowOf(null)

    override fun retry(post: Post) = Unit

    override fun openVideo(id: String) = Unit
}

@Inject
internal class DefaultPostCardsComponent(
    private val github: GitHubRepository,
    private val youtube: YouTubeRepository,
    private val posts: PostRepository,
    private val scope: CoroutineScope,
) : PostCardsComponent {
    override fun gitHub(ref: GitHubRef): Flow<GitHubCard?> = github.card(ref)

    override fun youTube(videoId: String): Flow<YouTubeVideo?> = youtube.video(videoId)

    override fun quote(id: PostId): Flow<Post?> = flow {
        var fetched = false
        posts.observePost(id).collect { post ->
            if (post == null && !fetched) {
                fetched = true
                scope.launch { posts.refreshPost(id) }
            }
            emit(post)
        }
    }

    override fun retry(post: Post) {
        scope.launch { posts.retryPublish(post.id) }
    }

    /** Navigation belongs to the root, which wraps this component. */
    override fun openVideo(id: String) = Unit
}
