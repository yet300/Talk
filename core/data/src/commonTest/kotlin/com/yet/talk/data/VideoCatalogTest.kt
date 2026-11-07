package com.yet.talk.data

import com.yet.talk.data.video.catalog.BundledVideoCatalog
import com.yet.talk.data.video.catalog.VIDEO_CATALOG_SIZE
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoCatalogTest {

    /** The chunks join into one JSON array with every video, so no chunk boundary or escape broke it. */
    @Test
    fun theBundledCatalogIsOneJsonArrayOfEveryVideo() {
        val videos = Json.parseToJsonElement(BundledVideoCatalog.catalogJson()).jsonArray
        assertEquals(VIDEO_CATALOG_SIZE, videos.size)
        assertTrue(videos.all { it.jsonObject["vid"]?.jsonPrimitive?.content?.length == 11 })
    }
}
