package com.yet.talk.domain.model.user

/**
 * The fields the profile editor changes. A blank text field removes it from the profile; a null [picture] or
 * [banner] keeps the current one. Every other field already in the profile (set by other Nostr apps, like
 * `lud16`) is kept.
 */
data class ProfileEdit(
    val name: String? = null,
    val displayName: String? = null,
    val about: String? = null,
    val picture: String? = null,
    val banner: String? = null,
    val website: String? = null,
    val nip05: String? = null,
    /** A GitHub account verified in the editor; null keeps the current link. */
    val github: com.yet.talk.domain.model.github.GitHubAccount? = null,
)
