package com.yet.talk.swift

import com.yet.talk.domain.model.activity.ActivityItem
import com.yet.talk.domain.model.channel.LocationChannel
import com.yet.talk.domain.model.chat.Chat
import com.yet.talk.domain.model.chat.ChatMessage
import com.yet.talk.domain.model.post.Post
import com.yet.talk.domain.model.user.User
import com.yet.talk.feature.channels.ChannelsComponent
import com.yet.talk.feature.chat.component.newchat.NewChatComponent
import com.yet.talk.feature.tab.activity.ActivityComponent
import com.yet.talk.feature.tab.bookmarks.BookmarksComponent
import com.yet.talk.feature.tab.chats.ChatsComponent
import com.yet.talk.feature.tab.home.HomeComponent
import com.yet.talk.feature.tab.main.MainComponent
import com.yet.talk.feature.tab.videos.VideosComponent
import com.yet.talk.feature.profile.ProfileComponent
import com.yet.talk.paging.PagingController
import com.yet.talk.resources.toDateText
import com.yet.talk.resources.toRelativeDesc
import dev.icerock.moko.resources.desc.StringDesc
import kotlin.time.Instant

/*
 * What Swift cannot read on its own. Ids are value classes, which Objective-C sees as opaque objects, so lists and
 * comparisons in SwiftUI use these string keys; flows of pages are handed over as [PagingController]s of their type.
 */

/** The post's id as text: stable across revisions, for list identity and selection. */
val Post.key: String get() = id.value

val User.key: String get() = id.value

val Chat.key: String get() = id.value

val ChatMessage.authorKey: String get() = author.value

val ChatMessage.Quote.authorKey: String get() = author.value

/** The post a details panel was opened with, for the highlighted card next to it; null for a profile or video. */
val MainComponent.Link.postKey: String? get() = (this as? MainComponent.Link.Post)?.id?.value

/** The chat a pane belongs to (the conversation, its info or Verify); null for New chat and channels. */
val ChatsComponent.Pane.chatKey: String?
    get() = when (this) {
        is ChatsComponent.Pane.Conversation -> id.value
        is ChatsComponent.Pane.Info -> id.value
        is ChatsComponent.Pane.Verify -> id.value
        is ChatsComponent.Pane.NewChat, is ChatsComponent.Pane.Channels -> null
    }

/** One row of Activity: who did what, when. */
val ActivityItem.key: String get() = "${type.name}:${actor.id.value}:${createdAt.toEpochMilliseconds()}:${eventPost?.id?.value ?: subjectPost?.id?.value}"

/** Someone without inbox relays, who cannot be picked. */
fun NewChatComponent.Model.cannotReceive(user: User): Boolean = user.id in cannotReceive

/** The person whose inbox relays are being checked. */
fun NewChatComponent.Model.isChecking(user: User): Boolean = checking == user.id

/** People in [channel] in the last five minutes; null where nobody announces ("? people"). */
fun ChannelsComponent.Model.peopleIn(channel: LocationChannel): Int? = people[channel.geohash]

/** `now`, `5m`, `2h`, `3d`, then `Oct 28`. */
fun Instant.relativeText(): String = toRelativeDesc().localizedText()

/** `Oct 28`, with the year once it differs. */
fun Instant.dateText(): String = toDateText()

fun HomeComponent.FeedPage.pagingController(): PagingController<Post> = PagingController(posts)

fun ProfileComponent.TabPage.pagingController(): PagingController<Post> = PagingController(posts)

fun BookmarksComponent.pagingController(): PagingController<Post> = PagingController(posts)

fun ActivityComponent.pagingController(): PagingController<ActivityItem> = PagingController(items)

fun VideosComponent.browseController(): PagingController<com.yet.talk.domain.model.video.Video> = PagingController(browse)

private fun StringDesc.localizedText(): String = localized()
