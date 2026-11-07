# UI review: the Claude Design file "Talk"

Source: the Claude Design canvas "Talk" (49 artboards, X (Twitter) design system), read in full on
2026-11-05: every screen artboard, every component artboard, their scripts (what is interactive)
and the canvas notes (purpose, actions and the iPhone Duo rules). This file is the contract for
the UI phase: **build what is listed here, nothing else**. Anything missing is listed under
"Not in the design" and "Open questions" and waits for a decision.

Product decisions that override the design (made earlier, see `AGENTS.md`):

- Posts are KarpathyTalk posts: one markdown text, kind 1. The title and excerpt are derived from the
  markdown (`Post.summary`). The design's long-form fields (separate Title, Summary, Tags; `naddr`;
  "Long-form articles are addressable") do not apply.
- Revisions are our kind 1010 events, fetched from relays, not "saved on this device" snapshots.
- Sign-in is a local key for now (create or import `nsec`). Signers (NIP-07, NIP-46 bunker, signer
  app) come later.
- Activity is a fifth tab. Search has its own tab and the data layer behind it.

## 1. Size classes

| Class | Viewport | Artboards | Chrome |
|---|---|---|---|
| **Phone** | 390×844 (any width < 688) | `*-Mobile` | top bar, bottom nav capsule (glass), FAB |
| **Duo outer** | 466×678 | `*-Outer` | 60 px side column on the left + one pane |
| **Duo inner sideways** | 951×669, fold at x = 475 | `*-Inner` | side column + two panes split exactly on the fold (415 + 476) |
| **Duo inner upright** | 669×951, fold at y = 475 | `Timeline-Upright` | the phone layout, wider |
| **Duo half open** | 669×951 | `Compose-HalfOpen` | content above the fold, controls below, nothing tappable on the fold |
| **Web** | 1280×900, fluid | `*-Web`, `Main` | left rail, main column (600), right column (350) |

Web breakpoints (`talk.css`):

| Width | Rail | Right column |
|---|---|---|
| ≥ 1225 | 275 px, icons + labels, "Compose" text button | shown |
| 1018–1224 | 68 px, icons only, Compose becomes a 56 px icon button | shown |
| 688–1017 | 68 px, icons only | hidden |
| ≤ 687 | becomes a bottom bar (sticky), no brand, no "me" row | hidden, columns go full width |

Duo rules from the canvas notes:

- The camera side is on the left in sideways, so the side column sits there.
- Folding keeps selection, scroll position, open article and draft. (Decompose state + `StateKeeper`
  cover this; scroll positions must be kept per pane.)
- Sideways panes split exactly on the fold, so each pane is about the size of the outer screen.

## 2. Navigation chrome

### Phone (`Timeline-Mobile`, `MobileNav`)
- **Top bar (home only)**: avatar 32 px → *own profile*; wordmark "Talk"; settings icon → *Settings*.
- **Top bar (other screens)**: back icon → *pop*; title (screen name or the user's name).
- **Bottom nav** (glass capsule, 4 items, icon only, `aria-current` on the active one): Home, Search,
  Bookmarks, Profile. ⚠ Activity is not in it (see Open questions).
- **FAB** (56 px, bottom right, above the nav): compose icon → *Compose (new)*. Home only.

### Web (`Rail`)
- Brand "Talk" → Home.
- Items: Home, Bookmarks, Profile, Settings (icon + label, active = bold). ⚠ No Search, no Activity:
  on web, search is the field at the top of the right column.
- "Compose" button (52 px, blue pill) → *Compose (new)*.
- Identity row at the bottom: avatar, name, `npub… · N relays` → *Settings*.
- `active="none"` on Reader, Revisions and Compose.

### Duo (`SideColumn`, 60 px)
- Optional Back (top) → *pop*.
- Contextual actions under it: Timeline/Bookmarks/Profile: **Compose**. Reader (outer): **Like,
  Bookmark, Reply, Revisions** (the article's own action row is hidden there: `chrome="side"`).
- **More actions** button (always). ⚠ Its content is not designed.
- Tabs at the bottom: Home, Bookmarks, Profile (active one on a glass pill). ⚠ No Search, no Activity.

## 3. Screens

Notation: **control** → action → component API (✗ = missing in the feature layer).

### 3.1 Sign in (`AuthPanel`; Auth-Mobile, Auth-Web, Auth-Outer)
Layout: one centred panel, max 420 px (web: centred on the page; phone: centred vertically; outer:
top padding 32).
- Wordmark, h1 "Sign in with your Nostr key", subtitle.
- **Primary**: web "Connect browser extension" (NIP-07), iOS "Open signer app". ✗ not in scope (signers later).
- **Use a remote signer** (web secondary; on iOS the bunker field is always open) + bunker field +
  Connect. ✗ not in scope.
- Link "Don't have a key? Get a signer" (external). ✗ not in scope.
- Toggle "Paste a private key instead" → reveals: password field "Private key" (`nsec1…`), caution
  caption, **Use this key** → `onKeyChanged` / `onImportKey`.
- ⚠ Our feature also has **Create a new key** (`onCreateKey`); the design has no such button.
- States: error under the field (`error`, from `DomainError`), busy (`isWorking`).

### 3.2 Timeline (Timeline-Mobile, Main (web), Timeline-Outer, Timeline-Inner, Timeline-Upright, Timeline-Popular)
- Header: web "Home" (sticky bar); phone: the home top bar (2).
- **Feed tabs**: Following · Popular · Latest → `HomeComponent.onFeedSelected`. Default Following.
- Popular only: caption "Ranked from what your follows and relays show you."
- List of **post cards** (3.10), paged. Duo panes use the dense card (excerpt 2 lines, no tags row).
- Duo inner sideways: feed | article. The selected card is highlighted (`t-selected`) and the right
  pane shows that post's article.
- Web right column: search field (→ Search, see 3.8) and a "Keep reading" widget (title, author ·
  read time, excerpt, **Keep reading** → open post). ✗ Which post goes there is not defined (see questions).
- States (`Feed`, `States-Mobile`): **loading** = 4 skeleton cards; **empty Following** = "Nothing from
  people you follow yet", body, **Browse Latest** → `onFeedSelected(Latest)`; **offline** = notice
  "Offline. Showing saved articles." above the list ✗ (no connectivity state in the domain).

### 3.3 Reader (Reader-Mobile, Reader-Web, Reader-Outer, Reader-Inner; `Article`, `Replies`)
- Header: phone: back + spacer + **More actions** ✗ (content not designed); web: back + "Article".
- **Article**: h1 title (from markdown) → author row: avatar (→ profile), name, `handle · date · N min
  read`, **Follow** ✗ (`PostComponent` has no follow) → action row (sticky): **Reply** (count)
  → focus the reply field / `onReplyInEditor`, **Repost** (count) → `onRepost`, **Like** (count,
  pink when liked) → `onLike`, **Bookmark** (blue when saved) → `onBookmark`, **Share** ✗ (platform
  share or copy link), **Revisions** → `onOpenRevisions` → body markdown (17/28 reading style:
  h2, p, lists, links, inline code, code block) → tags `#tag` → **Copy event address** (`naddr…`;
  for us: `nevent`/`note`) ✗ (no bech32 id on `Post`).
- **Replies**: "N replies" heading; inline composer (own avatar, field "Write a reply", **Reply**
  disabled until text) → `onReplyTextChanged` / `onSendReply`; reply cards (avatar, name, handle ·
  time, text, **Reply** count, **Like** count; max width 200 for actions).
- Layouts: phone = article then replies below; web = article in main, replies in the right column;
  Duo outer = article + replies in one pane, actions in the side column (Like, Bookmark, Reply,
  Revisions); Duo inner = **replies | article**.
- Duo rule: opening from the timeline must not move or reload the article: same pane, same layout,
  same scroll position. Only the left pane changes, from feed to replies, and Back appears in the side
  column.
- ⚠ Not designed: Quote, Edit, Delete (own posts), quoted post embed, reply-to context, images in
  the body, deleted/unavailable post, pending/failed publish badge.

### 3.4 Compose (Compose-Mobile, Compose-Web, Compose-Outer, Compose-Inner, Compose-HalfOpen; `Composer`)
- Bar: **Close** (X) → `onClose`; segmented **Edit · Split · Preview** → `onViewChanged` (phone and
  outer: Edit · Preview only); **Publish** → `onPublish`.
- Edit column: Title input ✗ (posts derive the title, see decisions); "Summary and tags · Saved on this
  device" toggle with Summary/Tags fields ✗ (does not apply); body textarea (monospace 15/24) → `onTextChanged`.
- Preview column: rendered markdown.
- Layouts: web = rail + wide main (max 1005), split 1fr 1fr; Duo inner = split across both panes
  (415 | 476); half open = preview above the fold (422 px), editor below; phone/outer = one view at a time.
- Partial publish state: alert row "Published to 5 of 7 relays. 2 timed out." + **Retry** ✗ (the
  component has `error` only; no per-relay result, no retry).
- "The draft saves locally on every keystroke and survives any fold": fold yes (StateKeeper);
  "saved on this device" (persistent draft) ✗.
- ⚠ Not designed: images (attach button, thumbnails), the reply/quote/edit target shown above the
  editor, character counter, publishing progress.

### 3.5 Profile (Profile-Mobile, Profile-Web, Profile-Outer, Profile-Inner; `ProfileHeader`)
- Bar: back + user name (web, phone).
- **Full header**: banner ✗ (no `banner` in `User`), 134 px avatar, **Follow** → `onFollowToggle`,
  name, `nip05 · npub…`, about, stats `N articles · N following · N followers` (`UserStats`).
- **Compact header** (Duo; "Outer drops the banner, because height is scarce"): 48 px avatar, name,
  handle, Follow, about, `N articles · N followers`.
- Tabs **Articles · Replies** → `onTabSelected(Posts|Replies)`; list of post cards.
- Duo inner: profile + feed | article (same list-detail as the timeline).
- ⚠ Not designed: own profile (Follow on yourself? edit profile?), empty tabs, loading header.

### 3.6 Revisions (Revisions-Mobile, Revisions-Web, Revisions-Limited; `RevisionList`)
- Bar: back + "Revisions".
- List groups: "Published" (Published version, `date · Latest on your relays`) / "On this device" /
  "Found on relays"; row = date, hint, diff `+212 −38`; selected row highlighted → `onSelect(number)`.
  For us: one group of revisions from relays (newest first), hint = the revision's title, diff
  computed from the previous revision ✗ (mapper).
- Selected snapshot: notice "*date* · Read-only" + **Published version** → back to latest
  (`onSelect(latest)`), then the article at that revision (no actions, no follow).
- Footer caption "This may not be the full history…".
- **Limited** state (only the latest exists): icon, "No earlier versions found", body.
- Layouts: phone = list on top, snapshot below; web = snapshot in main, list in the right column.
  Duo uses the phone artboard.

### 3.7 Bookmarks (Bookmarks-Mobile, Bookmarks-Web, Bookmarks-Inner, Bookmarks-Empty)
- Title "Bookmarks"; post cards; remove = the bookmark action on the card (`onBookmark`).
- Duo inner: list | article.
- **Empty**: bookmark icon 48, "Nothing saved yet", body, **Back to Home** → Home tab ✗ (no output for it).
- Copy rule: make no privacy claim ("only you", "only on this device") unless bookmarks are private.

### 3.8 Search (Search-Mobile; web: the right-column field)
- Search field in the bar (phone).
- Sections "People" (rows: avatar, name, handle → profile) and "Articles" (post cards).
- ⚠ Not designed: idle state, searching, no results, web results page (where results appear on web).

### 3.9 Settings (Settings-Mobile, Settings-Web; `SettingsPanel`)
- Bar: back + "Settings" (phone); web: "Settings" title (rail item active).
- **Identity**: avatar, name, `npub… · <signer type>`, **Sign out** → `onSignOut` (⚠ no confirmation designed).
- **Relays**: rows (icon relay/offline, URL, status "Connected · Read and write" / "Read only" /
  "Not responding · Retrying") ✗ (no connection status in the domain), remove icon → `onRemoveRelay`;
  add row: field `wss://` + **Add** → `onRelayInputChanged` / `onAddRelay`.
- **Theme**: tabs Dark · Light · System ✗ (no theme setting in the feature layer).
- ⚠ Our feature has **export key** (show `nsec`); the design has no such control.

### 3.10 Post card (`PostCard`)
Avatar (→ profile), name, `handle · time`, **title** (link → post), excerpt (3 lines, 2 when dense),
`N min read #tag #tag` (hidden when dense), actions: Reply (count), Repost (count, green when
reposted), Like (count, pink + filled when liked), Bookmark (blue + filled when saved). Selected
state (`t-selected`) in list-detail.
- ⚠ Not designed: repost header ("X reposted"), quote embed, reply context, images, pending/failed.
- ⚠ Reply on a card: in the design it is just a button; our components route it to the editor
  (`onReply` → Compose Reply). Quote has no button anywhere.

## 4. Shared UI components to build (`ui/components`)

Avatar (sizes 32/40/48/134, initials fallback), Button (primary, secondary, post, icon), Tabs
(underline, `x-tabs`), Segmented (editor view), Input/Search field, Top bar, Bottom nav, Rail, Side
column, FAB, Post card (+ dense, selected), Action button (reply/repost/like/bookmark/share), Skeleton
card, Empty state, Notice row, Alert row, Section title, List row, Markdown view, Relay row.

## 5. Not in the design (do not build without a decision)

- Dialogs: none. Sheets: none. Menus: none (the **More actions** button exists with no content).
- Toasts/snackbars: none (errors only appear as the publish alert row and inline).
- Screens: Activity, Docs, edit profile, onboarding, image viewer, followers/following lists.
- Confirmations: delete, sign out, remove relay.

## 6. Gaps in the feature layer (from the design)

| Gap | Where | Needed for |
|---|---|---|
| Large screens: list-detail panes | Main/Root | Duo inner, web reader/revisions |
| Follow the author | `PostComponent` | Reader author row |
| Share / copy event address (`nevent`) | `Post` / `PostComponent` | Reader |
| Per-relay publish result + Retry | `ComposeComponent`, `Publisher` | Compose partial publish |
| Persistent draft | `ComposeComponent` | "Saved on this device" |
| Theme setting | `SettingsComponent` + storage | Settings → Theme |
| Relay connection status | `RelayRepository` | Settings relay rows |
| Offline state | domain + Home | Offline notice |
| Banner | `User` / kind 0 | Profile header |
| "Back to Home" from empty Bookmarks | `BookmarksComponent.Output` | Empty state |
| Revision diff stats and titles | revisions mapper | Revision rows |

## 7. Proposed large-screen navigation (Decompose `childPanels`)

`MainComponent` keeps the tabs in the **main** panel and opens a post in the **details** panel
(`ChildPanels`; mode set by the UI from the window size: SINGLE on phone, Duo outer and narrow web;
DUAL on Duo inner sideways). Opening a post in DUAL mode keeps the list and shows the article on the
right with the card selected; "reader" mode swaps the left pane to that post's replies (same
`PostComponent`, so the article does not reload). In SINGLE mode the details panel covers the main
panel, which is the phone/web behaviour. Compose, Settings, Revisions and Auth stay root stack
destinations; someone else's profile moves into the main panel's stack so Duo shows profile | article.
Web keeps SINGLE and uses its right column for replies (reader) and the revision list (revisions).

## 8. Decisions (2026-11-05)

1. **Activity**: a navigation item (bell icon `ic_activity`) on the phone nav, web rail and Duo
   side column. Rows reuse the Search "People" row: avatar, "<name> liked your post" (strings
   `activity_*`), time; a tap opens the post or the profile.
2. **Search**: a rail item and a side-column item on web and Duo; results open in the main column,
   the same screen as on phone. The right-column field on web leads there too.
3. **More actions**: Quote, Copy link; for own posts also Edit and Delete (Delete asks in a confirm
   dialog). A dropdown menu on web, a bottom sheet on phone and Duo. Repost reposts directly.
4. **Sign in**: the designed panel without signer buttons; the private key section is open by
   default with **Use this key** as the primary button, plus a secondary **Create a new key**.
5. **Composer**: no Title and no Summary/Tags; one markdown body (title = its first heading),
   Edit/Split/Preview, image attach, and the reply/quote/edit target above the editor.
6. **Duo reader**: tapping a card shows its article on the right and keeps the list on the left;
   Reply (or the reply count) in the article swaps the left pane to the replies and shows Back.
7. **Settings**: the three designed sections, plus **Show private key** under Identity (warning +
   copy), and a stored Dark/Light/System theme setting applied on every platform.
8. **Web timeline right column**: the search field only; no "Keep reading" widget.
9. **Phone navigation** (web below 688 px, and the phone apps): **Home, Search, Activity, Videos**. No
   Profile item: your profile opens from the avatar in the home header, Settings from its gear.
   **Bookmarks is a tab of your own profile** (Posts · Replies · Bookmarks). The web rail and the Duo side
   column keep Bookmarks and Profile. (2026-11-05; the canvas gets it in design pass 2,
   `claude-design-prompt-2.md`, and the code follows the canvas.)

## 9. Design passes 2 and 3 (2026-11-05)

The canvas now has 108 artboards. New and changed screens, each with an "Actions:" note on the canvas (the
notes are the contract; read the artboard when building its screen):

| Area | Boards | What to build |
|---|---|---|
| Section A fixes | all screens | posts wording, key-only sign-in, editor without title/summary, relay revisions, navigation, Settings, More actions |
| Save your key | `Auth-SaveKey-*`, `SaveKey` | after Create a new key, the `nsec` once: Copy, "I saved it" (enabled after Copy or 3 s), then Home |
| Sign out | `Settings-*` dialog | "Sign out?" with Show private key + Copy inside, red Sign out, Cancel |
| Edits | cards, reader, `Compose-Contexts` | "Edited" opens Revisions; edit mode line "Edits show in Talk. Other Nostr apps keep showing the original." |
| More actions | `More-Actions`, `MoreFlow`, `Menu`, `BottomSheet`, `ConfirmDialog` | Quote, Copy link (+ toast), Edit, Delete; Report with the public warning |
| States | `States-Sheet`, `StateSet`, `Toast` | Sending…, Not sent + Retry, search idle / no results, feed error + Retry, "Link copied" toast |
| Search | `Search-Web`, `Search-Outer`, `Search-Inner` | a page of its own on web and Duo |
| Activity | `Activity-*`, `ActivityList` | as built, plus Duo inner list \| post |
| Your profile | `Profile-Own-*`, `Bookmarks-Mobile(-Empty)` | Edit profile instead of Follow; Phone: Posts · Replies · Bookmarks |
| Edit profile | `EditProfile-*`, `ProfileEditor`, `EditProfile-Upload` | banner, avatar (GIF ≤ 1 MB), fields, NIP-05 Verified; upload states incl. rejected by the on-device check |
| GitHub | `GitHub-States`, `GitHubLink`, `RepoCard` | NIP-39 linking via gist (not linked, waiting, verified, failed); repo and user cards |
| Moderation | `Moderation` | blurred avatars/banners outside your network (Show), GIFs animate only for follows, hidden rows for content your follows reported |
| Cards | `PostCard-Variants`, `MediaGrid`, `ImageViewer`, `LinkRow`, `VideoCard` | article card for `# heading` posts, text card (6 lines, Show more) otherwise; repost header, reply context, media grid 1–4, quote, one link card (YouTube, GitHub, else a plain row), Share |
| Videos | `Videos-*`, `VideoPage-*`, `VideoHome`, `VideoPlayer`, `VideoInfo` | HackerTube catalog: chips Videos/Liked/Watch later/History, categories, Level and Type menus; video page with player, Like, Watch later, Share, Copy link, why HN, comments (NIP-22), related; credit line |

Decisions in these passes:

10. **Phone capsule**: Home, Search, Activity, Videos (see 9 above). **Duo inner upright** uses the same
    capsule as Phone and no Videos header button (the canvas still shows the old one there).
11. Things the canvas leaves to code: the **GitHub mark** comes from Octicons (`mark-github`); **thumbnails**
    are `i.ytimg.com/vi/<vid>/hqdefault.jpg` (`mqdefault.jpg` on small cards); the five video summaries the
    designer wrote are placeholders, the catalog's own summaries are used; like and comment counts come from
    Nostr.
12. **Security** (designer's notes, agreed): a strict Content-Security-Policy on the web, markdown stays
    sanitized, signers stay on the roadmap.
13. **Avatars and banners come only from sites that moderate them** (2026-11-06, replaces the uploads with
    the on-device check): GIPHY and Unsplash links, pasted in a small dialog (Avatar link / Banner link, Open
    GIPHY, Open Unsplash, Link field, Use, Cancel) that the banner and avatar in the editor open; a GIPHY page
    link becomes its GIF. Talk also *shows* avatars and banners only from these hosts (plus
    `avatars.githubusercontent.com`, set only from a verified GitHub link in phase 3); any other URL, set by
    another client or a script, shows the placeholder. The list is `ProfileImageHosts`. An image Talk does not
    show is never removed by saving the profile. The editor caption reads "Avatar and banner come from GIPHY
    or Unsplash. Banner: 1500 × 500." The upload states (uploading, rejected, Retry) are not built.
14. **GitHub** (2026-11-06): `GitHubLink` sits under NIP-05 in the editor and is saved with Save. Once
    verified it also offers **Use GitHub avatar** (`avatars.githubusercontent.com/<login>`), the only way to
    set a GitHub avatar. The profile header shows the GitHub badge only for a checked claim. `RepoCard`
    shows in post cards (not in the reader, whose markdown already has the link); while it loads, or when
    GitHub has none or is rate limited, the `LinkRow` shows. The GitHub mark is Octicons `mark-github-24`.
15. **Cards** (2026-11-06): a post starting with `# heading` is an article card, any other a text card
    (plain text, 6 lines, Show more when it is longer than 6 lines or 420 characters). One link card per
    post: the first YouTube link, else the first GitHub repository or user, else the first link as a row.
    A YouTube card plays inline in YouTube's privacy-enhanced player (`youtube-nocookie.com`, the only
    `frame-src` in the CSP); until the Videos phase the phone plays inline too. Following shows each post
    once, at its latest entry: its own time, or the time someone you follow reposted it ("Name reposted").
    Replies show "Replying to @name". Not sent shows Retry in place of the actions, which publishes the
    stored event again.
16. **Videos** (2026-11-06): the catalog is compiled into the apps (refreshed weekly by CI) and loads into the
    database on the first Videos or video page ("Getting the catalog ready…"). For you ranks videos sharing
    categories with Liked and History, then by HackerTube's score, and never shows one of those; three cards.
    Related: most categories in common, three. Browse pages 20 at a time; the category chips are all 150, the
    largest first. Comments are top-level only and their cards have no actions. Copy link copies the YouTube
    URL; Share opens the editor with it, so the post gets a YouTube card. On the phone a video card in a post
    opens the video page; on the web it plays in the card. Phone navigation is Home, Search, Activity, Videos.
    On the phone For you is a row to swipe and the categories one swipeable row, so the filters are on the first
    screen; wider, the categories wrap: the 12 largest, then More (all 150) and Fewer.
17. **Phone navigation shows only on its four tabs** (2026-11-07). Every page above them (a post, a profile, a
    video, your profile, Bookmarks, Settings, Revisions, the editors) has Back in its top bar instead; Back on
    your profile and on Bookmarks returns Home.
18. **Moderation** (2026-11-07): the network is the viewer and the people they follow. Outside it, avatars and
    banners are blurred (avatars everywhere; Show on a profile's banner reveals both) and GIPHY GIF avatars
    show their still frame (`giphy_s.gif`). Posts reported (NIP-56) by the network or by the viewer are hidden
    behind "Hidden because people you follow reported it" with Show, in cards and replies. Report is in More
    actions on someone else's post and in … on someone else's profile: a menu with the public-report note and
    Nudity, Spam, Illegal, Impersonation, Other, then the "Reported. Your report is public." toast. The
    viewer's own report hides the post for them at once. Reported profiles are not hidden.

### Build order

1. Accounts and polish: Save your key, the Sign out dialog, "Edited" → Revisions, the edit-mode line, the
   Link copied toast, the web CSP.
2. Profiles: Edit profile (banner, fields, image links from allowed sites (decision 13), NIP-05), your
   profile header with banner, website and Edit profile, the Bookmarks tab on Phone.
3. GitHub: NIP-39 linking and verification; repo and user cards in posts.
4. Cards: text and article variants, media grid and viewer, quote, repost header, reply context, link rows,
   YouTube cards.
5. Videos: the catalog in the database, Videos and the video page, comments and likes on Nostr, the lists,
   For you; the Phone capsule switches to Videos.
6. Moderation: network-based blurring, reports (NIP-56) and hidden content.

All six are built (2026-11-07).

## 10. Design pass 4: glass app bars and Chats (2026-11-07)

Prompt: [claude-design-prompt-3.md](claude-design-prompt-3.md); research: [../research/chats.md](../research/chats.md).

| Area | Boards | What they add |
|---|---|---|
| Glass bars | `Glass-Scrolled`, `Glass-States`, `GlassBarDemo`, `Light-Glass-Scrolled`; the Home, Profile, Post, Videos, video page and Settings boards | every top bar is glass over edge-to-edge content; Home's tabs a floating glass capsule; Profile's banner under a transparent bar that turns to glass with the name after ~160 px; the scroll-edge shadow past 4 px; the solid fallback |
| Chats | `ChatList`, `Chats-Mobile/Web/Outer/Upright/Inner`, `Chats-Web-Menu`, `Conversation`, `Chat-Mobile/Group-Mobile/Request-Mobile/Outer/Upright`, `Chats-States-1/2` | list with Chats · Requests and the Location channels row; conversation with bubbles, reply quotes, images, reactions, message menu (sheet on Phone, hover menu on Web), Sending… / Not sent, request bar (Accept, Delete, Block); empty states, offline, no inbox relays |
| New chat, info, verify | `NewChat*`, `ChatInfo*`, `ChatInfo-Encryption`, `VerifyContact`, `Verify-Mobile`, `Verify-Verified` | people search (up to 10, a group name from 2), chat info (verify, disappearing messages, mute, block, report, leave), the encryption explainer sheet, safety code and QR |
| Location channels | `LocationChannels`, `Channels-*`, `ChannelsIntro`, `Channels-Intro`, `ChannelRoom`, `Channel-Room-Mobile`, `Channel-Teleported-Mobile`, `Teleport*` | five levels with counts ("? people" for neighborhood and block), first run and the location step, nickname, room, teleport by area code or map |
| Security | `SecurityPanel`, `Security-States`, `AppLock`, `Lock-Mobile`, `Lock-Web` | Settings' Security section: app lock, hide in screenshots (Android), erase chats, erase everything; the lock screen |
| Light | `Light-Chats-*` | light versions |

Glass materials (designer's values, to add to `TalkTokens` and the colours when building):

- `glass-bar`: dark `rgba(0,0,0,.55)`, light `rgba(255,255,255,.62)`, `backdrop-filter: blur(20px) saturate(180%)`.
- `glass-btn` (round buttons): dark `rgba(32,35,39,.72)`, light `rgba(255,255,255,.8)`, with `shadow-glass-sm`
  (dark `inset 0 0 0 1px rgba(255,255,255,.1), inset 0 1px 0 rgba(255,255,255,.16), 0 2px 8px rgba(0,0,0,.5)`;
  light `inset 0 0 0 1px rgba(0,0,0,.06), inset 0 1px 0 rgba(255,255,255,.8), 0 2px 8px rgba(0,0,0,.12)`).
- Scroll edge: a 24 px gradient under the bar, dark `rgba(0,0,0,.8)` to 0, light `rgba(255,255,255,.6)` to 0,
  hidden at rest, shown past 4 px in 200 ms ease-out.
- Capsule: `glass-bg`, the glass filter, `shadow-glass`, the active item `glass-pill`.
- Reduced transparency: `glass-solid`, no blur (the system setting; on the web `prefers-reduced-transparency`,
  which only Chromium supports, so also a `t-solid-bars` class on `<html>`).

Decisions in this pass (on the designer's notes):

19. **Phone navigation is five items:** Home, Search, Activity, Videos, Chats, with an unread count (replaces
    decision 9's four). Web: Chats in the rail after Activity, with the count. Duo: Chats in the side column
    with a dot. Web Chats is two panes (list | conversation), the one exception to the web's single pane.
20. **The encryption explainer says** "…can see that someone sent you a message and when it arrived, but not
    who sent it or what it says." (the designer's correction: relays see arrival times and the connection that
    publishes).
21. **Block channels warn:** the Block level adds "People here can tell you're within about a block." (a
    7-character geohash is about 150 m, and a relay can tie it to an IP address).
22. **Outgoing bubbles use `#0A6BC2`** (a new colour, `bubble-out`), not `x-blue`: white on `x-blue` is 3.0:1,
    under the 4.5:1 that message text needs; `#0A6BC2` is 5.4:1. Links and buttons keep `x-blue`.
23. **Leave group only hides it on this device** (a NIP-17 group is its set of keys); the confirmation says "You
    won't see this group here. Its members can still message it." Delete for everyone keeps "Other apps may
    still show it".
24. **Web app lock encrypts the key:** the passcode encrypts the stored key with NIP-49 (`ncryptsec`, scrypt +
    XChaCha20-Poly1305), so the key cannot be used until it is entered; a forgotten passcode means signing in
    again with the `nsec` backup. Android and iOS use the device lock (biometrics or passcode) to unlock KSafe.
25. **Reactions are emoji** sent as the reaction's content (NIP-25 inside the wrap); the safety-code words, area
    codes and counts on the canvas are samples.
26. **Copy the designer wrote is accepted:** the channels explainer and nickname line, the Teleport text, "No
    requests", the "Can't receive chats yet" banner, "Talk is locked", "If the codes don't match, don't mark as
    verified."

### Build order

1. Glass app bars on the web: tokens, the bars and the Home capsule, edge-to-edge lists, Profile's banner bar,
   the scroll-edge shadow, the solid fallback.
2. Chats core: NIP-44 v2, NIP-59 seal and gift wrap, NIP-17 text messages, the kind 10050 inbox relays (yours
   published, theirs read), the list with Chats · Requests, the conversation, Sending… / Not sent, offline,
   "Can't receive chats yet"; the fifth navigation item with the unread count; messages encrypted at rest.
3. Chats in full: groups up to 10, replies, reactions, delete for everyone / for me, encrypted images (kind 15,
   AES-GCM on Blossom), disappearing messages (NIP-40), mute and block (NIP-51 kind 10000), report, leave.
4. Trust and security: verify a contact (safety code, QR), Settings' Security section, app lock (NIP-49 on the
   web), erase chats, erase everything.
5. Location channels: bitchat-compatible kinds 20000 and 20001, a throwaway key per area, nicknames, presence
   at region, province and city only, teleport, the location step, optional proof of work (NIP-13).
