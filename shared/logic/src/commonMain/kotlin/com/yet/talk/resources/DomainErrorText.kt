package com.yet.talk.resources

import com.yet.talk.domain.model.error.DomainError
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc

/**
 * The message shown for an error. Android: `toString(context)`, iOS: `localized()`,
 * web: `toLocalizedString(provider)`.
 */
fun DomainError.toStringDesc(): StringDesc = when (this) {
    DomainError.EmptyContent -> StringDesc.Resource(MR.strings.error_empty_content)
    is DomainError.ContentTooLong -> StringDesc.ResourceFormatted(MR.strings.error_content_too_long, max)
    DomainError.InvisibleCharacters -> StringDesc.Resource(MR.strings.error_invisible_characters)
    is DomainError.TooManyImages -> StringDesc.ResourceFormatted(MR.strings.error_too_many_images, max)
    is DomainError.ImageTooLarge -> StringDesc.ResourceFormatted(MR.strings.error_image_too_large, maxBytes / (1024 * 1024))
    is DomainError.UnsupportedImageType -> StringDesc.Resource(MR.strings.error_unsupported_image)
    is DomainError.AboutTooLong -> StringDesc.ResourceFormatted(MR.strings.error_about_too_long, max)
    DomainError.ImageHostNotAllowed -> StringDesc.Resource(MR.strings.error_image_host)
    DomainError.InvalidGitHubLogin -> StringDesc.Resource(MR.strings.error_github_login)
    DomainError.InvalidGistLink -> StringDesc.Resource(MR.strings.error_gist_link)
    DomainError.GitHubProofMissing -> StringDesc.Resource(MR.strings.github_proof_missing)
    DomainError.InvalidKey -> StringDesc.Resource(MR.strings.error_invalid_key)
    DomainError.InvalidRelayUrl -> StringDesc.Resource(MR.strings.error_invalid_relay)
    DomainError.NotSignedIn -> StringDesc.Resource(MR.strings.error_not_signed_in)
    DomainError.NotFound -> StringDesc.Resource(MR.strings.error_not_found)
    DomainError.NoRelayAccepted -> StringDesc.Resource(MR.strings.error_no_relay_accepted)
    is DomainError.Network -> StringDesc.Resource(MR.strings.error_network)
}
