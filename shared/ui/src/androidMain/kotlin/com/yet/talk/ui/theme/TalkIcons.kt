package com.yet.talk.ui.theme

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import com.yet.talk.resources.*
import dev.icerock.moko.resources.ImageResource

/** The design's 24×24 stroke icon set (moko `images/ic_*.svg`). Filled variants mark an active state. */
object TalkIcons {
    val Home = MR.images.ic_home
    val Search = MR.images.ic_search
    val Activity = MR.images.ic_activity
    val Bookmark = MR.images.ic_bookmark
    val BookmarkFilled = MR.images.ic_bookmark_filled
    val User = MR.images.ic_user
    val Compose = MR.images.ic_compose
    val Reply = MR.images.ic_reply
    val Repost = MR.images.ic_repost
    val Like = MR.images.ic_like
    val LikeFilled = MR.images.ic_like_filled
    val Share = MR.images.ic_share
    val History = MR.images.ic_history
    val Back = MR.images.ic_back
    val More = MR.images.ic_more
    val Settings = MR.images.ic_settings
    val Close = MR.images.ic_close
    val Relay = MR.images.ic_relay
    val Key = MR.images.ic_key
    val Copy = MR.images.ic_copy
    val Check = MR.images.ic_check
    val Plus = MR.images.ic_plus
    val Offline = MR.images.ic_offline
    val Chat = MR.images.ic_chat
    val Send = MR.images.ic_send
    val Info = MR.images.ic_info
    val Timer = MR.images.ic_timer
    val Lock = MR.images.ic_lock
    val Mute = MR.images.ic_mute
    val Block = MR.images.ic_block
    val Users = MR.images.ic_users
    val Shield = MR.images.ic_shield
    val Qr = MR.images.ic_qr
    val Pin = MR.images.ic_pin
    val Teleport = MR.images.ic_teleport
    val Chevron = MR.images.ic_chevron
    val Image = MR.images.ic_image
    val Trash = MR.images.ic_trash
}

/** One icon, tinted with the content colour by default; [contentDescription] null for decoration. */
@Composable
fun TalkIcon(
    icon: ImageResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.sizes.iconNav,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icon.drawableResId),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
