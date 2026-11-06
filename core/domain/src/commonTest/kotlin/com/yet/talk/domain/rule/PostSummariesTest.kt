package com.yet.talk.domain.rule

import kotlin.test.Test
import kotlin.test.assertEquals

class PostSummariesTest {

    @Test
    fun usesTheFirstHeadingAsTitleAndTheFirstParagraphAsExcerpt() {
        val summary = PostSummaries.of(
            """
            ## nanochat jan7 miniseries

            I need to test out **image upload** so here is the [nanochat](https://github.com/karpathy/nanochat) miniseries.

            ![chart](https://example.com/a.png)
            """.trimIndent(),
        )
        assertEquals("nanochat jan7 miniseries", summary.title)
        assertEquals("I need to test out image upload so here is the nanochat miniseries.", summary.excerpt)
    }

    @Test
    fun fallsBackToTheFirstLineWithoutAHeading() {
        val summary = PostSummaries.of("Short thought about relays.\n\nAnd a second paragraph.")
        assertEquals("Short thought about relays.", summary.title)
        assertEquals("And a second paragraph.", summary.excerpt)
    }

    @Test
    fun collectsHashtagsOutsideCodeAndHeadings() {
        val summary = PostSummaries.of(
            "# Title\n\nWriting about #Nostr and #rust_lang, see issue #12.\n\n```\nlet a = #notATag\n```",
        )
        assertEquals(listOf("nostr", "rust_lang", "12"), summary.tags)
    }

    @Test
    fun estimatesReadingTimeAtTwoHundredWordsAMinute() {
        assertEquals(1, PostSummaries.of("one two three").readMinutes)
        assertEquals(3, PostSummaries.of(List(401) { "word" }.joinToString(" ")).readMinutes)
    }

    @Test
    fun capsLongTitlesAndExcerpts() {
        val summary = PostSummaries.of("# " + "t".repeat(300) + "\n\n" + "e".repeat(1000))
        assertEquals(PostSummaries.MAX_TITLE, summary.title.length)
        assertEquals(PostSummaries.MAX_EXCERPT, summary.excerpt.length)
    }

    @Test
    fun textCardsKeepParagraphsAndArticlesStartWithATitle() {
        val text = PostSummaries.of("Fast feedback is **better**.\nSecond line.\n\n![x](https://e.x/a.png)\n\nLast [link](https://e.x).")
        assertEquals("Fast feedback is better.\nSecond line.\n\nLast link.", text.text)
        assertEquals(false, text.isArticle)
        assertEquals(true, PostSummaries.of("# Title\n\nBody").isArticle)
        assertEquals(false, PostSummaries.of("## Small heading\n\nBody").isArticle)
    }
}
