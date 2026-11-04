package com.yet.talk.domain.rule

import com.yet.talk.domain.model.DomainError
import com.yet.talk.domain.model.ImageFile
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ported from `content_sanitize_test.go`, plus the limits from `handlers.go`. */
class ContentRulesTest {

    @Test
    fun normalizesLineEndingsAndStripsControls() {
        assertEquals("hello\nworld\tok\ndone", ContentRules.sanitize("hello\r\nworld\u0000\tok\rdone").getOrThrow())
    }

    @Test
    fun rejectsInvisibleSpoofingCharacters() {
        listOf("bad‮content", "bad⁦content", "bad​content", "bad﻿content").forEach {
            assertEquals(DomainError.InvisibleCharacters, ContentRules.sanitize(it).exceptionOrNull(), it)
        }
    }

    @Test
    fun rejectsUnpairedSurrogatesButKeepsEmoji() {
        assertEquals(DomainError.InvisibleCharacters, ContentRules.sanitize("bad\uD800x").exceptionOrNull())
        assertEquals("ok 🚀", ContentRules.sanitize("ok 🚀").getOrThrow())
    }

    @Test
    fun enforcesPostAndReplyLimitsInCodePoints() {
        assertEquals(DomainError.EmptyContent, ContentRules.validatePost(" \n\t", isReply = false).exceptionOrNull())
        assertEquals("a".repeat(10_000), ContentRules.validatePost("a".repeat(10_000), isReply = false).getOrThrow())
        assertEquals(DomainError.ContentTooLong(10_000), ContentRules.validatePost("a".repeat(10_001), isReply = false).exceptionOrNull())
        assertEquals(DomainError.ContentTooLong(5_000), ContentRules.validatePost("a".repeat(5_001), isReply = true).exceptionOrNull())
        // 5000 emoji are 10000 UTF-16 units but 5000 characters.
        assertEquals(true, ContentRules.validatePost("🚀".repeat(5_000), isReply = true).isSuccess)
    }

    @Test
    fun validatesImages() {
        assertEquals(true, ContentRules.validateImage(ImageFile(ByteArray(10), "image/png")).isSuccess)
        assertEquals(DomainError.UnsupportedImageType("image/svg+xml"), ContentRules.validateImage(ImageFile(ByteArray(10), "image/svg+xml")).exceptionOrNull())
        assertEquals(DomainError.ImageTooLarge(ContentRules.MAX_IMAGE_BYTES), ContentRules.validateImage(ImageFile(ByteArray(5 * 1024 * 1024 + 1), "image/jpeg")).exceptionOrNull())
        assertEquals(DomainError.TooManyImages(4), ContentRules.validateImageCount(5).exceptionOrNull())
    }
}
