package com.yet.talk.domain.model.post

/** What the signed-in user did to a post. All false when signed out. */
data class ViewerState(val liked: Boolean = false, val reposted: Boolean = false, val bookmarked: Boolean = false)
