package com.yet.talk.domain.model.error

import com.yet.talk.domain.model.post.Image
import com.yet.talk.domain.model.post.Post

/** Failures the UI can explain. Returned inside [Result]. */
sealed class DomainError(message: String) : Exception(message) {

    sealed class Validation(message: String) : DomainError(message)

    data object EmptyContent : Validation("Post is empty")

    data class ContentTooLong(val max: Int) : Validation("Post is longer than $max characters")

    data object InvisibleCharacters : Validation("Post contains invisible or bidi control characters")

    data class TooManyImages(val max: Int) : Validation("A post can have at most $max images")

    data class ImageTooLarge(val maxBytes: Long) : Validation("Image is larger than $maxBytes bytes")

    data class UnsupportedImageType(val mimeType: String) : Validation("Unsupported image type: $mimeType")

    data class AboutTooLong(val max: Int) : Validation("About is longer than $max characters")

    /** Avatars and banners come only from the hosts in [com.yet.talk.domain.rule.ProfileImageHosts]. */
    data object ImageHostNotAllowed : Validation("Image link is not from an allowed site")

    data object InvalidKey : Validation("Not a valid nsec or secret key")

    data object InvalidRelayUrl : Validation("A relay address starts with wss://")

    data object NotSignedIn : DomainError("Sign in to do this")

    data object NotFound : DomainError("Not found")

    /** No relay confirmed the write. */
    data object NoRelayAccepted : DomainError("No relay accepted the event")

    data class Network(override val cause: Throwable? = null) : DomainError("Network error")
}
