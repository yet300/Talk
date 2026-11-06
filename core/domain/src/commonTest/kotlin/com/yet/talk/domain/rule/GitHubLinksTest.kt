package com.yet.talk.domain.rule

import com.yet.talk.domain.model.github.GitHubRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubLinksTest {

    @Test
    fun findsTheFirstRepositoryOrUserLink() {
        assertEquals(GitHubRef.Repo("ines-okafor", "relaydiff"), GitHubLinks.find("Try [it](https://github.com/ines-okafor/relaydiff). Also https://github.com/sam"))
        assertEquals(GitHubRef.User("sam"), GitHubLinks.find("Read https://github.com/sam, always"))
        assertEquals(GitHubRef.Repo("a", "b"), GitHubLinks.find("https://www.github.com/a/b.git"))
    }

    @Test
    fun skipsPagesCodeAndDeepLinks() {
        assertNull(GitHubLinks.find("https://github.com/features and https://github.com/a/b/issues/1"))
        assertNull(GitHubLinks.find("```\nhttps://github.com/a/b\n```\n`https://github.com/c/d`"))
        assertNull(GitHubLinks.find("https://github.com.evil.example/a/b"))
    }

    @Test
    fun readsGistLinks() {
        assertEquals("abc123abc123abc123ab", GitHubLinks.gistId("https://gist.github.com/Me/ABC123abc123abc123ab"))
        assertEquals("abc123abc123abc123ab", GitHubLinks.gistId(" https://gist.github.com/abc123abc123abc123ab/ "))
        assertNull(GitHubLinks.gistId("https://gist.github.com.evil/x/abc123abc123abc123ab"))
        assertNull(GitHubLinks.gistId("http://gist.github.com/x/abc123abc123abc123ab"))
    }
}

class PostLinksTest {

    @Test
    fun prefersYouTubeThenGitHubThenAnyLink() {
        assertEquals(
            com.yet.talk.domain.model.post.PostLink.YouTube("VMj-3S1tku0", "https://youtu.be/VMj-3S1tku0"),
            PostLinks.find("Code https://github.com/a/b and the talk https://youtu.be/VMj-3S1tku0."),
        )
        assertEquals(com.yet.talk.domain.model.post.PostLink.GitHub(GitHubRef.Repo("a", "b")), PostLinks.find("See https://example.org/x and https://github.com/a/b"))
        assertEquals(com.yet.talk.domain.model.post.PostLink.Web("https://example.org/notes/fsync"), PostLinks.find("Good: https://example.org/notes/fsync."))
        assertNull(PostLinks.find("![chart](https://example.com/a.png) and `https://x.y`"))
    }

    @Test
    fun readsYouTubeLinkShapes() {
        listOf(
            "https://www.youtube.com/watch?v=VMj-3S1tku0&t=10", "https://m.youtube.com/watch?feature=x&v=VMj-3S1tku0",
            "https://youtube.com/shorts/VMj-3S1tku0", "https://www.youtube-nocookie.com/embed/VMj-3S1tku0",
        ).forEach { assertEquals("VMj-3S1tku0", PostLinks.youTubeId(it), it) }
        assertNull(PostLinks.youTubeId("https://youtube.com.evil/watch?v=VMj-3S1tku0"))
        assertNull(PostLinks.youTubeId("https://www.youtube.com/@karpathy"))
    }
}
