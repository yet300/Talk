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
| display name, avatar, bio | kind 0 metadata (NIP-01) |
| GitHub link | NIP-39 external identity (`["i", "github:<login>", "<proof gist id>"]`) |

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
