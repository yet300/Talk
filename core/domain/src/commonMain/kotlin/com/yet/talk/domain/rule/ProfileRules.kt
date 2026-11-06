package com.yet.talk.domain.rule

import com.yet.talk.domain.model.error.DomainError
import com.yet.talk.domain.model.user.ProfileEdit

/** Limits of the profile editor (Claude Design "Talk", ProfileEditor). */
object ProfileRules {
    const val MAX_ABOUT_LENGTH = 160

    /**
     * Trims and sanitises every text field like posts; the about line is at most [MAX_ABOUT_LENGTH]. A new
     * picture or banner must be a link [ProfileImageHosts] accepts.
     */
    fun validate(edit: ProfileEdit): Result<ProfileEdit> = runCatching {
        fun String?.clean(): String? = this?.let { ContentRules.sanitize(it).getOrThrow().trim() }?.takeIf(String::isNotEmpty)
        fun String?.image(): String? = this?.let { ProfileImageHosts.normalize(it) ?: throw DomainError.ImageHostNotAllowed }
        val about = edit.about.clean()
        if (about != null && about.length - about.count { it.isLowSurrogate() } > MAX_ABOUT_LENGTH) {
            throw DomainError.AboutTooLong(MAX_ABOUT_LENGTH)
        }
        ProfileEdit(
            name = edit.name.clean()?.replace('\n', ' '),
            displayName = edit.displayName.clean()?.replace('\n', ' '),
            about = about,
            picture = edit.picture.image(),
            banner = edit.banner.image(),
            website = edit.website.clean(),
            nip05 = edit.nip05.clean()?.lowercase(),
        )
    }
}

enum class ProfileImage { Avatar, Banner }
