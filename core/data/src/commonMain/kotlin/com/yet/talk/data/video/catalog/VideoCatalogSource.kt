package com.yet.talk.data.video.catalog

/** The HackerTube catalog as one JSON array of videos, trimmed to what Talk shows. */
interface VideoCatalogSource {
    /** Changes when the catalog does. */
    val version: Long

    fun catalogJson(): String
}

/** The catalog compiled into the app (`VideoCatalogPart*.kt`, refreshed by scripts/update_video_catalog.py). */
internal object BundledVideoCatalog : VideoCatalogSource {
    override val version: Long = VIDEO_CATALOG_VERSION

    override fun catalogJson(): String = videoCatalogJson()
}
