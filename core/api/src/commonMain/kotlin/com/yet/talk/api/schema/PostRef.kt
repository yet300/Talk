package com.yet.talk.api.schema

/** A post as other events point at it: its id, its author ("" if unknown) and, if it was edited, the revision that was seen. */
data class PostRef(val id: String, val author: String, val revisionId: String? = null)
