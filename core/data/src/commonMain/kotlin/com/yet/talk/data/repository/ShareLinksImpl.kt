package com.yet.talk.data.repository

import com.yet.talk.domain.model.post.PostId
import com.yet.talk.domain.repository.ShareLinks
import com.yet.talk.nostr.encoding.Nip19
import dev.zacsweers.metro.Inject

@Inject
internal class ShareLinksImpl : ShareLinks {
    override fun post(id: PostId): String = Nip19.note(id.value)
}
