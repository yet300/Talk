package com.yet.talk.domain.rule

import com.yet.talk.domain.model.post.PostSummary
import kotlin.math.ceil

/**
 * Derives a card's title, excerpt, tags and reading time from a post's markdown. KarpathyTalk posts
 * are plain markdown documents, so the first heading (or first line) stands in for a title.
 */
object PostSummaries {
    const val MAX_TITLE = 100
    const val MAX_EXCERPT = 280
    private const val WORDS_PER_MINUTE = 200

    fun of(markdown: String): PostSummary {
        val prose = markdown.lines().withoutCodeBlocks()
        val blocks = prose.joinToString("\n").split(Regex("\n\\s*\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val heading = blocks.firstOrNull { HEADING.matches(it.lineSequence().first()) }
        val title = (heading?.lineSequence()?.first()?.replace(HEADING_PREFIX, "") ?: blocks.firstOrNull()?.lineSequence()?.first())
            ?.let(::plain).orEmpty()
        val excerpt = blocks
            .filterNot { it == heading || HEADING.matches(it.lineSequence().first()) }
            .map(::plain)
            .firstOrNull { it.isNotEmpty() && it != title }
            .orEmpty()
        val words = WORD.findAll(prose.joinToString(" ")).count()
        return PostSummary(
            title = title.take(MAX_TITLE),
            excerpt = excerpt.take(MAX_EXCERPT),
            tags = HASHTAG.findAll(prose.joinToString(" ")).map { it.groupValues[1].lowercase() }.distinct().toList(),
            readMinutes = ceil(words / WORDS_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1),
            github = GitHubLinks.find(markdown),
        )
    }

    private fun List<String>.withoutCodeBlocks(): List<String> {
        var inCode = false
        return filter { line ->
            if (line.trimStart().startsWith("```")) {
                inCode = !inCode
                false
            } else {
                !inCode
            }
        }
    }

    /** Markdown to one line of text: images dropped, links and emphasis unwrapped. */
    private fun plain(block: String): String = block
        .replace(IMAGE, "")
        .replace(LINK, "$1")
        .replace(INLINE_MARKS, "")
        .replace(LIST_MARK, "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private val HEADING = Regex("^#{1,6}\\s+.*")
    private val HEADING_PREFIX = Regex("^#{1,6}\\s+")
    private val IMAGE = Regex("!\\[[^\\]]*\\]\\([^)]*\\)")
    private val LINK = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
    private val INLINE_MARKS = Regex("[*_`~]+")
    private val LIST_MARK = Regex("^\\s*(?:[-*+]|\\d+\\.|>)\\s+", RegexOption.MULTILINE)
    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val HASHTAG = Regex("(?<![\\w#&])#([\\p{L}\\p{N}_]+)")
}
