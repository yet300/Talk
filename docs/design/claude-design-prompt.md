# Prompt for Claude Design: update the "Talk" canvas

Paste everything below the line into Claude Design, on the existing canvas
https://claude.ai/artifact/DfFymLRByEHfUcREbTDFjX ("Talk", X (Twitter) design system).

---

You are updating the existing **Talk** canvas (https://claude.ai/artifact/DfFymLRByEHfUcREbTDFjX). Keep its design
system (X (Twitter), already installed), its tokens, its components (`PostCard`, `Feed`, `Article`, `Replies`,
`Composer`, `ProfileHeader`, `RevisionList`, `SettingsPanel`, `Rail`, `MobileNav`, `SideColumn`, `AuthPanel`,
`Icon`) and its size classes. Change what section A lists, add what sections B–E describe, and nothing else: do
not invent screens, flows or features that are not written here.

## Context

Talk is a serverless take on KarpathyTalk (https://karpathytalk.com, now offline): a small network for builders
where posts are markdown, with likes, reposts, quotes, replies, follows, bookmarks and revisions. Everything is
stored on **Nostr**: public relays hold the data, the apps are the only code, there is no backend of ours.

- The **web app is built** from this canvas (Compose HTML, using this file's CSS). Android (Compose) and iOS
  (SwiftUI, including iPhone Duo) come next and will follow the same artboards.
- Nostr limits that shape the design:
  - No central server: no global moderation, no server-side search ranking, no push. Anything "smart" runs on
    the device or is computed from what relays return.
  - **Link previews:** a browser cannot read other sites' Open Graph data (CORS) without a server. YouTube
    (oEmbed) and GitHub (REST API) work, so they get rich cards; any other link shows as a plain link row with
    the domain. Do not draw rich previews for arbitrary sites.
  - **Profiles** (kind 0) can be set by any Nostr app, so we cannot stop someone from using an explicit avatar;
    we only control what our app uploads and what it shows (see B).
- Size classes stay as they are: Phone 390×844, Web 1280×900 (fluid, the existing breakpoints), Duo outer
  466×678, Duo inner sideways 951×669 (fold at x = 475), Duo inner upright and half open 669×951. Both themes.
  Keep the canvas notes' Duo rules (camera side left; folding keeps selection, scroll, open item and draft).

## A. Bring the canvas in line with decisions already taken

These are decided; update the existing artboards and their notes to match.

1. **Posts** are KarpathyTalk posts: one markdown text (Nostr kind 1). The card's title is the first `# heading`;
   say "posts", not "articles", everywhere ("12 posts", tabs "Posts · Replies", search section "Posts").
2. **Sign in:** signers (browser extension, remote signer, signer app) come later, so for now: the AuthPanel
   with the private key section **open by default**, "Use this key" as the primary button, and a secondary
   **Create a new key**. Remove the signer buttons, the bunker field and "Get a signer".
3. **Editor:** no Title field and no "Summary and tags" fields. One markdown body (its first heading is the
   title), Edit · Split · Preview (Phone and Duo outer: Edit · Preview), an **Add image** button in the bar, the
   attached images as a strip of thumbnails with a remove button, the **character counter** (`170 / 10000`),
   and the post being replied to, quoted or edited shown above the body. "Save" instead of "Publish" when
   editing. Keep the partial publish alert with Retry.
4. **Revisions** come from relays (our own revision events), not "saved on this device": one group "Published"
   (the latest) and one group "Earlier versions" (date, the version's title, `+12 −3` lines). Keep the limited
   state.
5. **Navigation:**
   - **Phone** bottom capsule: **Home, Search, Activity, Bookmarks** (four items, no Profile). Your profile
     opens from the avatar in the home header, Settings from its gear, as now.
   - **Web rail:** Home, **Search**, **Activity**, Bookmarks, Profile, Settings, the Compose button, the
     identity row (avatar, name, `npub… · N relays`).
   - **Duo side column** tabs: Home, Search, Activity, Bookmarks, Profile.
   - Web timeline right column: the search field only. **Remove the "Keep reading" widget.**
6. **Activity** (no artboard yet): a list of rows in the Search "People" row style: avatar, "*Name* liked your
   post" / "replied" / "quoted your post" / "reposted your post" / "followed you", then time · the post's
   title; a row opens the post (or the profile for a follow). Empty state with the bell icon. Draw Phone, Web,
   Duo inner (list | the opened post).
7. **More actions** (the `…` button in the reader bar and the Duo side column): **Quote, Copy link**, and on
   your own post **Edit, Delete**. A dropdown menu (`x-menu`) on the web, a bottom sheet on Phone and Duo.
   **Delete** asks in a confirm dialog: "Delete this post?", "Relays are asked to delete it. Some may keep a
   copy.", red Delete, Cancel.
8. **Settings:** Identity row `npub… · Private key on this device` with Sign out (ask in a confirm dialog),
   then **Show private key**: reveals the `nsec` in a mono box with a warning ("Anyone with this key can post
   as you. Keep it secret."), Copy and Hide. Relays rows say "Read and write" / "Read only". Theme: Dark ·
   Light · System. Add **Edit profile** under Identity (see B).
9. **States the canvas lacks** (one small sheet of them is enough, Phone + Web): a post that is **Sending…**
   and one that is **Not sent** (with Retry); search **idle** ("Search people and posts by name, words or
   tags.") and **no results**; a feed **error row** with Retry; a **toast** ("Link copied"); the reply field
   while sending.

## B. Profile editing (new)

- **Edit profile** opens from your own profile header (a secondary "Edit profile" button where others see
  Follow) and from Settings. Phone: full screen with Cancel and Save in the bar; Web: a modal (`x-modal`,
  600 wide); Duo inner: a right pane next to your profile.
- Fields: **banner** (1500×500 area, tap to change), **avatar** (circle over the banner, tap to change;
  **GIFs allowed**, up to 1 MB), **Name**, **Display name**, **About** (multi-line, counter), **Website**,
  **NIP-05** (`you@domain`, with a "verified" check when it resolves), **GitHub** (see D).
- Upload states for avatar and banner: uploading (progress ring on the image), done, **rejected by the safety
  check**: "This image can't be used. Talk checks images on your device before they're uploaded." (no image
  is uploaded), and a network error with Retry.
- **What other people see** (moderation without a server, draw these on the profile header, post card and
  reply):
  - Avatars and banners of people **outside your network** (you don't follow them and none of your follows
    do) show **blurred**, with a small "Show" on the banner; GIF avatars play only for people you follow,
    otherwise their first frame.
  - Content **reported by people you follow** (a "nudity" or "spam" report) is hidden behind a row:
    "Hidden because people you follow reported it · Show".
  - A **Report** item in More actions on someone else's post and profile: a sheet/menu with Nudity, Spam,
    Illegal, Impersonation, Other; then "Reported. People who follow you will see this."
- Profile header: banner image (or the surface colour when none), avatar, name, handle, about, **website**
  link and the **GitHub** badge (D), stats `posts · following · followers`.

## C. Videos from HackerTube (new)

HackerTube (https://hackertube.org, post: https://karpathy.bearblog.dev/hacker-tube/) is Andrej Karpathy's
catalog of 6,454 YouTube videos recommended in Hacker News comments, in 150 topics. Its data is open
(https://hackertube.org/data, ODC-By 1.0: **credit HackerTube** wherever the catalog shows). Each video has:
title, channel, YouTube id (thumbnail `i.ytimg.com`), summary, "why HN" (why Hacker News recommends it), a
quote from an HN comment, content type (talk, course, lecture, documentary…), level (intro, intermediate,
advanced), shelf life (evergreen…), primary topic, categories, tags, and HN stats (mentions, recommenders).
Videos play in the YouTube embedded player. Talk adds what HackerTube lacks: **likes and comments on Nostr**
(anyone on Nostr commenting on the same video URL shows up), sharing a video into a post, and per-user lists.

Design:
1. **Videos** as a new destination: a **Videos** item in the web rail and Duo side column, and on Phone a
   **"Videos" entry in Home** (a tab next to Following · Popular · Latest, or a header button; pick one and
   note why). Do not add a fifth phone nav item.
2. **Videos home:** "For you" (computed on the device from your likes and history, so a new user sees the most
   recommended), then topic chips (categories), and a grid (Web: 2–3 columns; Phone: one column) of **video
   cards**: 16:9 thumbnail with duration if known, title (2 lines), channel, `topic · level`, and the "why HN"
   line in secondary text. Filters: topic, level, content type. Lists: **Liked**, **Watch later**, **History**
   (all stored on the device; Liked is also a public Nostr reaction).
3. **Video page:** the 16:9 player; title; channel; `N likes · N comments`; actions **Like, Watch later, Share
   (to a post), Copy link**; summary; a "Why Hacker News recommends it" block with the HN quote and
   "N recommenders"; tags; **comments** (same style as Replies, with the reply field); **related videos**
   (same topic). A small credit line: "From HackerTube · data ODC-By 1.0". Layouts: Phone (player on top,
   content scrolls), Web (player and comments in the main column, related in the right column), Duo inner
   sideways (player | comments), Duo half open (player above the fold, controls and comments below).
4. **Sharing a video into a post:** the editor gets a video attachment (paste a YouTube link or "Share" from
   the video page); in the feed, a post with a video shows a **video card** (thumbnail with a play badge,
   title, channel), playing inline on Web and opening the video page on Phone.
5. Empty states: no likes yet, nothing to watch later, no comments ("Be the first to comment").

## D. GitHub (new)

1. **GitHub on the profile** (Nostr NIP-39): in Edit profile, "Link GitHub": enter the username → Talk shows a
   short text to put in a **public gist** → paste the gist link → "Verify". States: not linked, waiting for the
   gist, verified, failed ("The gist doesn't contain the text"). On the profile header: a GitHub badge with the
   username and a check when verified; it opens github.com/username.
2. **Repository cards in posts:** a link to `github.com/owner/repo` shows a card: owner avatar, `owner/repo`,
   description (2 lines), language dot, stars, forks. A link to a user shows avatar, name, bio, followers. A
   GitHub link that cannot be loaded (rate limit) falls back to the plain link row.

## E. Post cards closer to x.com (new)

Redesign `PostCard` (and the Replies cards) as a set of variants. The rule we want: **a post that starts with a
`# heading` keeps the article card** (title, excerpt, `N min read #tags`); **any other post shows its text
like on x.com** (up to ~6 lines with "Show more"). Both variants share:
- a **repost header** above the avatar ("*Name* reposted", repost icon, secondary text);
- a **reply context** line ("Replying to @handle");
- a **media grid** for 1–4 images like x.com (1: up to 510 high, rounded; 2: side by side; 3: one large + two
  small; 4: 2×2), GIFs with a "GIF" badge, tap opens an **image viewer** (dark scrim, swipe/arrow between
  images, close);
- **one link card** when the post has a link: YouTube → video card (C4), GitHub → repo card (D2), anything
  else → a plain link row (favicon-less globe icon, domain, the URL, no image);
- a **quoted post** embed (`x-quote`): the quoted author, time, and its title or text (3 lines) with its
  first image;
- the action row as now (Reply, Repost, Like, Bookmark) plus **Share**; counts like x.com.
Show the variants on the Components page and use them in Timeline, Profile, Bookmarks, Search and the reader's
replies, at Phone, Web and Duo.

## New icons (same stroke set: 24×24, 1.75 stroke, round caps, currentColor)

play, video, github, link, globe, star, fork, camera (change photo), report (flag), eye-off (hidden content),
clock (watch later), more-horizontal exists. Add them to the `Icon` component's list.

## Deliverables

- Update the existing artboards for section A and their canvas notes.
- New artboards: Activity (Phone, Web, Duo inner); Edit profile (Phone, Web modal, Duo inner); Videos home,
  Video page (Phone, Web, Duo outer, Duo inner sideways, half open); a States sheet (A9); moderation states on
  profile and cards (B); GitHub linking states (D1).
- Components page: PostCard variants (article, text, with media grid 1–4, with link card, with quote, repost
  header, reply context), VideoCard, RepoCard, LinkRow, MediaGrid, ImageViewer, Menu, BottomSheet,
  ConfirmDialog, Toast, ProfileEditor, the new icons.
- Light theme sheets for the new screens, like the existing "Light" page.
- For every new or changed screen, a canvas note in the existing style: one line of purpose, then
  "Actions: …" listing every control and what it does. The developers map each action to code, so a control
  without an action in the note will not be built.

Do not design: uploading your own videos, direct messages, notifications settings, onboarding, followers or
following lists, a Docs page, or rich previews for arbitrary websites.

Reference links:
- KarpathyTalk (original product): https://karpathytalk.com
- HackerTube: https://hackertube.org · data: https://hackertube.org/data · post: https://karpathy.bearblog.dev/hacker-tube/
- Nostr NIPs used: profiles https://github.com/nostr-protocol/nips/blob/master/24.md · GitHub identity
  https://github.com/nostr-protocol/nips/blob/master/39.md · comments on any URL
  https://github.com/nostr-protocol/nips/blob/master/22.md · reactions to external content
  https://github.com/nostr-protocol/nips/blob/master/25.md · external IDs
  https://github.com/nostr-protocol/nips/blob/master/73.md · reports
  https://github.com/nostr-protocol/nips/blob/master/56.md · content warnings
  https://github.com/nostr-protocol/nips/blob/master/36.md · image metadata
  https://github.com/nostr-protocol/nips/blob/master/92.md
- X (Twitter) post layout for the cards: https://x.com
