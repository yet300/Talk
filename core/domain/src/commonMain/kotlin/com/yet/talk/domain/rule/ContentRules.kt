package com.yet.talk.domain.rule

import com.yet.talk.domain.model.DomainError
import com.yet.talk.domain.model.ImageFile

/** KarpathyTalk's content limits and sanitising (`handlers.go`, `content_sanitize.go`). */
object ContentRules {
    const val MAX_POST_LENGTH = 10_000
    const val MAX_REPLY_LENGTH = 5_000
    const val MAX_IMAGES_PER_POST = 4
    const val MAX_IMAGE_BYTES = 5L * 1024 * 1024
    val ALLOWED_IMAGE_TYPES = setOf("image/png", "image/jpeg", "image/gif", "image/webp")

    /**
     * Normalises line endings to `\n`, strips control characters except `\n` and `\t`, and rejects
     * zero-width and bidi control characters (used to spoof text) and unpaired surrogates.
     */
    fun sanitize(content: String): Result<String> {
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        val builder = StringBuilder(normalized.length)
        var index = 0
        while (index < normalized.length) {
            val char = normalized[index]
            when {
                char.isHighSurrogate() -> {
                    val low = normalized.getOrNull(index + 1)
                    if (low == null || !low.isLowSurrogate()) return Result.failure(DomainError.InvisibleCharacters)
                    builder.append(char).append(low)
                    index++
                }
                char.isLowSurrogate() -> return Result.failure(DomainError.InvisibleCharacters)
                char == '\n' || char == '\t' -> builder.append(char)
                char.code < 0x20 || char.code == 0x7F -> Unit
                isBlockedInvisible(char) -> return Result.failure(DomainError.InvisibleCharacters)
                else -> builder.append(char)
            }
            index++
        }
        return Result.success(builder.toString())
    }

    /** Sanitises and checks the length; the result is what gets published. */
    fun validatePost(content: String, isReply: Boolean): Result<String> = sanitize(content).mapCatching { clean ->
        val max = if (isReply) MAX_REPLY_LENGTH else MAX_POST_LENGTH
        when {
            clean.isBlank() -> throw DomainError.EmptyContent
            clean.codePointCount() > max -> throw DomainError.ContentTooLong(max)
            else -> clean
        }
    }

    fun validateImage(file: ImageFile): Result<ImageFile> = when {
        file.mimeType !in ALLOWED_IMAGE_TYPES -> Result.failure(DomainError.UnsupportedImageType(file.mimeType))
        file.bytes.size > MAX_IMAGE_BYTES -> Result.failure(DomainError.ImageTooLarge(MAX_IMAGE_BYTES))
        else -> Result.success(file)
    }

    fun validateImageCount(count: Int): Result<Unit> =
        if (count > MAX_IMAGES_PER_POST) Result.failure(DomainError.TooManyImages(MAX_IMAGES_PER_POST)) else Result.success(Unit)

    private fun isBlockedInvisible(char: Char): Boolean =
        char == '​' || char == '‌' || char == '‍' || char == '﻿' ||
            char in '‪'..'‮' || char in '⁦'..'⁩'

    // Go limits count runes, not UTF-16 units: an emoji counts once.
    private fun String.codePointCount(): Int = length - count { it.isLowSurrogate() }
}
