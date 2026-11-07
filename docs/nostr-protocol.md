# Talk on Nostr: event schema

Talk recreates the feature set of KarpathyTalk (the original Go app lives in
`../KarpathyTalkApp/reference/KarpathyTalk-master`) on Nostr, without a server of our own.
The original model carries over unchanged: **there are users and there are posts**. A reply is
a post with a parent, a quote is a post that points at another post, and timelines are queries
over posts.

This document is the contract between `core:nostr`, the data layer and the features.
Treat a change here as a protocol change: existing events on relays cannot be migrated.

## Community boundary

KarpathyTalk is its own community, not "all of Nostr". Every event Talk publishes carries a
NIP-32 label namespace:

```json
["L", "com.yet.talk"],
["l", "post", "com.yet.talk"]
```

- Feeds filter by the namespace: `{"kinds": [1], "#L": ["com.yet.talk"]}`. Relays index
  single-letter tags, so this is a server-side filter.
- The `l` value names the role: `post`, `reply`, `revision`, `reaction`, `repost`.
- Events from other Nostr clients are not shown in Talk feeds. Profiles (kind 0) and follow
  lists (kind 3) are global Nostr objects and are reused as-is.
- The boundary is a convention, not access control: anyone can add the label. That matches
  KarpathyTalk, where anyone with a GitHub account could post.

## Identity

| KarpathyTalk | Talk |
|---|---|
| GitHub OAuth | A secp256k1 key pair (`nsec`/`npub`) |
| `username` | `npub`, plus an optional NIP-05 handle for a readable name |
| display name, avatar, bio | kind 0 metadata (NIP-01; `banner`, `website` from NIP-24) |
| GitHub link | NIP-39 external identity (`["i", "github:<login>", "<proof gist id>"]`) |

Editing kind 0: Talk fetches the latest kind 0 from relays and changes only the keys it edits (`name`,
`display_name`, `about`, `website`, `nip05`, and `picture`/`banner` when changed), keeping every other key
(`lud16`, other apps' fields). If relays return none while one is cached, the save fails rather than
overwrite it. The new event's `created_at` is newer than the one it replaces. `picture` and `banner` are set
only to GIPHY or Unsplash links, and Talk shows them only from those hosts and GitHub avatars
(`ProfileImageHosts`).

GitHub (NIP-39): the editor shows the proof text "Verifying that I control the following Nostr public key:
<npub>", the user puts it in a public gist, and Talk checks through GitHub's API that the gist's owner is the
claimed login and that it contains the text. The claim is saved with the profile; other `i` tags (other
platforms) are kept. Someone else's claim is checked when their profile is refreshed, at most once a day, and
shown only when the check holds; the result lives in the profile cache. GitHub links in posts get cards from
the API (repository or user), cached for a day. Without a token GitHub allows 60 requests an hour, so a
failure pauses fetching for five minutes and the plain link row shows meanwhile.

Videos (the HackerTube catalog): a video is named by its URL `https://www.youtube.com/watch?v=<id>` (NIP-73,
`k` = `web`), whatever link was shared. A like is a NIP-25 kind 17 reaction (`content` `+`, tags `k` and `i`,
Talk label role `video-like`); unliking publishes a NIP-09 deletion of it. A comment is a NIP-22 kind 1111 with
root and parent both the URL (`I`/`K` and `i`/`k`, role `video-comment`); Talk shows only these top-level
comments. Watch later and History stay on the device. Likes and comments from other clients on the same URL
count and show.

Moderation: a report is a public NIP-56 kind 1984 event (`["e", <post>, <type>], ["p", <author>]` for a post,
`["p", <pubkey>, <type>]` for a profile; Talk label role `report`), with type nudity, spam, illegal,
impersonation or other. Talk reads the reports made by the viewer and the people they follow (500 newest per
refresh, at sign-in) and hides the posts they report behind a row. Nothing is deleted or filtered on relays.

Key storage per platform:

- **Android:** an external signer (Amber, NIP-55) when installed, otherwise the `nsec` in KSafe.
- **iOS:** the `nsec` in KSafe (Keychain).
- **Web:** the `nsec` in KSafe (WebCrypto, non-extractable key in IndexedDB); a NIP-07 browser extension later.

## Posts

A post is a **kind 1** event whose `content` is the markdown source (GFM). Other Nostr clients
show it as plain text; Talk renders it as markdown.

```json
{
  "kind": 1,
  "content": "## nanochat jan7 miniseries\n\n...",
  "tags": [
    ["L", "com.yet.talk"],
    ["l", "post", "com.yet.talk"],
    ["imeta", "url https://…/abc.png", "m image/png", "x <sha256>"]
  ]
}
```

Limits (enforced by the client, as in KarpathyTalk):

- post body: 10,000 characters; reply body: 5,000 characters;
- the content is sanitised the same way as the original: CRLF → LF; control characters
  other than `\n` and `\t` are stripped; zero-width and bidi control characters are rejected.

## Revisions (Talk-specific)

Kind 1 is immutable, and replaceable kinds (30023) keep only the latest version. KarpathyTalk
keeps every revision, so Talk adds its own event:

**kind 1010: post revision**, a regular (non-replaceable) event that holds the full new markdown.

```json
{
  "kind": 1010,
  "content": "<full markdown of revision N>",
  "tags": [
    ["e", "<original post id>", "<relay hint>", "root"],
    ["revision", "2"],
    ["L", "com.yet.talk"],
    ["l", "revision", "com.yet.talk"]
  ]
}
```

Rules:

- The original kind 1 event is revision 1. Its id is the canonical post id (stable link).
- Revisions are fetched with `{"kinds": [1010], "#e": [postId], "authors": [author]}`. The
  `authors` filter is required: only the author's revisions count.
- The current content is the revision with the highest `revision` tag; ties are broken by
  `created_at`.
- A reply, quote or repost can pin the revision it saw: `["revision", "<revision event id>"]`.
  It is absent when the post had not been edited.
- Kind 1010 is not in the NIP kinds registry as of 2026-11. Check the registry again before the
  first public release.

## Social layer

| KarpathyTalk | Talk event |
|---|---|
| reply | kind 1 with NIP-10 `e` tags (`root`/`reply` markers) and `p` of the parent author |
| quote post | kind 1 with a `q` tag (NIP-18) |
| repost | kind 6 (NIP-18) |
| like | kind 7, `content: "+"` (NIP-25) |
| follow | kind 3 contact list (NIP-02) |
| bookmark | kind 10003 public bookmark list (NIP-51) |
| delete | kind 5 for the post and all of its revisions (NIP-09); relays may ignore it. A deletion only counts for the requester's own events, so clients keep tombstones per (event, author) |
| image upload | Blossom (BUD-01/02) and an `imeta` tag (NIP-92); 5 MB limit; PNG/JPEG/GIF/WebP |

Every event the app writes, apart from kinds 0, 3 and 10003, carries the label namespace.

## Private chats (NIP-17)

Chats are the standard Nostr DMs, so they work with other NIP-17 clients (0xchat, Amethyst, Coracle). They carry
no Talk label: the label would mark them on relays, and the inner events are never public.

| Layer | Event |
|---|---|
| message (the rumor) | kind 14, unsigned, `content` = the text, a `p` tag per other member; the id is computed but there is no `sig` |
| seal | kind 13, signed by the author, `content` = the rumor NIP-44 v2 encrypted to one recipient, no tags |
| gift wrap | kind 1059, signed by a throwaway key, `content` = the seal NIP-44 encrypted to that recipient, `["p", <recipient>]` |
| inbox relays | kind 10050 with `["relay", <url>]` tags (1 to 3) |

- **Sending:** one wrap per member, published to that member's kind 10050 relays, plus one to the author's own
  inbox (their copy on other devices). A member with no kind 10050 cannot be written to ("Can't receive chats
  yet"); nothing is sent to anyone else's relays instead.
- **Timestamps:** the seal's and the wrap's `created_at` are random, up to two days in the past (NIP-59), so
  relays do not learn when a message was written. The rumor keeps the real time.
- **Receiving:** a live subscription for kind 1059 with `#p` = me on my inbox relays, from the last sync minus two
  days. A wrap counts only when the seal's signature holds, the seal's pubkey equals the rumor's, and the rumor's
  id is right. The chat is the set of the rumor's `p` tags and author, minus me.
- **Inbox relays:** Talk publishes your kind 10050 when you first open Chats or send, if you have none:
  `wss://auth.nostr1.com` (serves wraps only to their recipient, after NIP-42 AUTH) and `wss://relay.damus.io`.
  Others' kind 10050 are read from their relays and from `wss://purplepag.es`.
- **NIP-42:** a relay that sends `AUTH` gets a kind 22242 event with `["relay", <url>]` and `["challenge", <c>]`,
  signed by the session key; subscriptions closed with `auth-required` are sent again once it says OK.
- **Groups:** up to 10 other people; every message is wrapped once per member. The chat is its set of
  members; a group's name travels as a `["subject", <name>]` tag on every message (the newest one wins). Leaving
  a group only hides it on this device.
- **Replies:** `["e", <message id>, "", "reply"]` on the kind 14.
- **Reactions:** a kind 7 rumor (`content` = the emoji, `["e", <message id>]`, `["k", "14"]`, every member as a `p`),
  wrapped like a message. One per person and message; the newest replaces the previous one.
- **Delete for everyone:** a kind 5 rumor with `["e", <message id>]` and every member as a `p`, wrapped like a
  message, published before the message is removed here. It counts only for the author's own messages in that
  chat. Other apps may ignore it. Delete for me only removes it on this device. Removed messages keep their row
  (content wiped), so syncing does not bring them back.
- **Images:** a kind 15 rumor (`content` = the Blossom URL) with `file-type`, `encryption-algorithm` `aes-gcm`,
  `decryption-key` and `decryption-nonce` (hex), `x` (SHA-256 of the ciphertext), `ox` (of the original) and
  `size`. The file is encrypted with a fresh AES-256-GCM key before upload (uploaded as
  `application/octet-stream`), and a download is decrypted only when its hash matches `x`.
- **Disappearing messages:** an `["expiration", <unix time>]` tag (NIP-40) on the rumor and on its wraps, set from
  the chat's setting (1 hour, 1 day, 1 week). Every device turns an expired message into "This message expired"
  and wipes its content; a message that arrives already expired is dropped. Copies other apps keep can't be recalled.
- **Requests:** a chat with nobody you follow, that you have neither written in nor accepted, is a request.
- **Block:** a private `["p", <pubkey>]` entry in the viewer's NIP-51 mute list (kind 10000): the private entries
  are a JSON array of tags in `content`, NIP-44-encrypted to yourself, so nobody else learns whom you blocked.
  Talk rewrites the list from its newest copy on relays and keeps every entry it does not change; a list whose
  private part is in the older NIP-04 format is not rewritten. Blocked people's chats and requests are hidden,
  and their posts sit behind a "Hidden because you blocked its author" row.
- **Mute** (a chat) is local: it leaves the chat out of the unread count.
- **On the device:** messages are stored NIP-44 encrypted with a random 32-byte key kept in KSafe
  (`chat_storage_key`), deleted on sign-out.
- **No forward secrecy** (NIP-17 has none): someone who later gets your key can read the wraps relays kept.

## Feeds

As in KarpathyTalk, timelines list posts only: reposts are counted on the post, not shown as timeline items.

| Tab | Query |
|---|---|
| Latest | kind 1 with `#L`, newest first; replies (events with an `e` tag) are dropped on the client |
| Following | the same filter plus `authors` = the kind 3 list; outbox relays from NIP-65 |
| Trending | computed on the client (see below) |
| Activity | `#p` = me, kinds 1/6/7 with `#L`. New followers cannot be queried reliably and are left out |
| Profile: posts / replies | `authors` = profile, kind 1 with `#L`, split by whether an `e` tag is present |
| Bookmarks | the items of the viewer's kind 10003 list |

Pagination uses `until` = the `created_at` of the last event on the page, which replaces the
`before={id}` cursor.

### Trending

The same formula as KarpathyTalk, computed on the client over events from the configured relays:

```
score = engagement / (age_hours + 2)^1.8
```

- Window: top-level posts from the last 7 days; keep the top 200.
- Engagement: likes (kind 7), reposts (kind 6) and replies (kind 1 with `e` = post), counting
  only events by someone other than the post's author.
- The result is cached locally and recomputed at most every 3 minutes.

## Counters

Like, repost and reply counts use NIP-45 `COUNT` where the relay supports it, and fall back to
counting fetched events locally.

## What the original server did, and where it went

| Server feature | Talk |
|---|---|
| JSON, Markdown and RSS read API | not needed: relay data is public and readable by any client or agent |
| rate limits | relay policies; NIP-13 proof of work can be added if spam appears |
| CSRF, sessions | not applicable: every event is signed |
| SQLite storage | relays are the source of truth; SQLDelight is the local cache |
