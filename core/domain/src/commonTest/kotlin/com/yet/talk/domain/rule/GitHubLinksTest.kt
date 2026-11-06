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
