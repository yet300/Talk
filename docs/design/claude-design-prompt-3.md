# Prompt for Claude Design, pass 4

Paste everything below the line into Claude Design on the same canvas
(https://claude.ai/artifact/DfFymLRByEHfUcREbTDFjX). Background for the second task:
[docs/research/chats.md](../research/chats.md).

---

Two tasks in this pass: glass app bars with edge-to-edge content on every screen, and a new feature, Chats.
Keep everything else as it is. As before: one note per new or changed board with "Actions: …" for every
control, light versions of anything new, and the Phone, Web and Duo (outer, inner upright, inner sideways)
size classes.

## 1. Glass app bars, content edge to edge

Today the top bars (Home's header with the avatar, Talk and the gear; the Following · Popular · Latest tabs;
the bars of Profile, Post, Search, Videos, Settings, Revisions, the editor, the video page…) are solid strips with
a 1 px line under them, and content starts below them. Make them match the glass capsule of the Phone
navigation (iOS 26 Liquid Glass and its counterparts on Android and the web):

- **Content runs edge to edge.** Lists, the profile banner, the video player and the article scroll under the
  top bar (and under the status bar on Phone), not below it. The first item starts below the bar at rest, then
  slides beneath it when scrolled.
- **The bar is glass, not a strip:** translucent material that blurs and tints what scrolls under it (use and, if
  needed, extend the existing tokens `glass-bg`, `glass-solid`, `glass-pill`, `glass-icon`, `shadow-glass`). No
  hard line under it: a **soft scroll-edge shadow** (a short gradient fade of the background, stronger in dark)
  appears only once content is under the bar.
- **Controls float as glass pieces.** Back, the avatar, the gear and other icons sit in round glass buttons; the
  title stays plain text on the glass; Home's Following · Popular · Latest become a glass segmented capsule.
- **Profile:** the banner reaches the top edge behind a transparent bar with glass Back and … buttons; the bar
  turns to glass with the name as the title once the banner scrolls away.
- **Video page and post with media:** the player and images may sit under the bar the same way.
- **Legibility:** show the bar over a busy image and over plain background, in dark and light; and the solid
  fallback when the system asks for reduced transparency (and on the web, `prefers-reduced-transparency`).
- **Web:** the main column's sticky header becomes the same glass with the scroll-edge shadow; the rail stays
  as it is.
- Show at rest and scrolled for: Home (Phone, Web), Profile (yours and someone else's, Phone and Web), Post
  (Phone), Videos and the video page (Phone), Settings (Phone), and one Duo board.

## 2. Chats

Private chats like X's Chat, with features from bitchat (its Nostr side, no Bluetooth). X Chat's weak point is
security: keys kept on X's servers behind a 4-digit PIN, no forward secrecy. Talk keeps the key only on the
device and has no PIN, so nothing in the design asks for one.

**Where it lives:**

- **Phone:** the navigation capsule becomes five items: Home, Search, Activity, Videos, **Chats** (a chat-bubble
  icon; show an unread badge).
- **Web:** a Chats item in the rail after Activity. Chats is two panes like X's Messages: the list on the left, the
  open conversation on the right (an exception to the web's single pane).
- **Duo:** outer and inner upright single pane; inner sideways list | conversation, split on the fold.

**Screens and states to draw:**

1. **Chats list.** Tabs **Chats** · **Requests** (chats from people you do not follow). Rows: avatar (group: two
   stacked avatars), name, last message preview, time, unread dot, verified check for verified contacts, a lock
   or timer glyph when disappearing messages are on. A **New chat** button. A **Location channels** entry at the
   top of the list (see 6). Empty state ("No chats yet", "Start a private chat with someone you follow.", New
   chat). Requests empty state.
2. **New chat:** search people (by name, NIP-05, npub); pick one for a chat, or several (up to 10) for a group
   with an optional group name. A person who cannot receive chats yet shows "Can't receive chats yet" and cannot
   be picked.
3. **Conversation:** X-style bubbles (yours right in `x-blue`, theirs left), grouped by sender and day with a date
   divider, sender names in groups. A message can carry a reply quote, an image (tap opens the viewer), and
   reactions (a small row under the bubble). Long press (Phone) or hover menu (Web): **Reply**, **React**,
   **Copy**, **Delete for everyone** (yours; note "Other apps may still show it"), **Delete for me**. States:
   sending, not sent with Retry (as on post cards), an expired message placeholder. Composer: text field, add
   image, send. A **Request** conversation shows "Name isn't someone you follow" with **Accept**, **Delete** and
   **Block** instead of the composer until accepted. No read receipts and no typing indicator (on purpose:
   they leak activity).
4. **Chat info** (from the header): the person or the group members; **Verify** (5); **Disappearing messages**
   (Off, 1 hour, 1 day, 1 week) with one line: "Messages disappear on your devices and are asked to expire on
   relays. Copies others keep can't be recalled."; **Mute**, **Block**, **Report** (reuse the public-report sheet);
   for groups **Leave**; and an **encryption explainer** row that opens a short sheet:
   - "Chats are end-to-end encrypted with your Nostr key. Your key stays on this device: no PIN, no copy on a
     server."
   - "Relays can see that someone sent you a message, but not who sent it, what it says, or when."
   - "If your key is ever exposed, older messages can be read. Disappearing messages limit that."
5. **Verify a contact:** a short safety code (for example, 6 words or 4 groups of digits) derived from both keys,
   shown on both phones, plus a QR code to scan in person; "Mark as verified" and the verified state.
6. **Location channels** (bitchat): public rooms for places, at five levels, Region, Province, City,
   Neighborhood, Block, each with its people count ("12 people"; finer levels show "? people", because people
   there do not announce themselves). A first-run explainer and the location permission step: "Talk turns your
   location into an area code on this device. Your exact location is never sent." Choosing a **nickname** for
   channels ("You appear with a new identity in each place"). **Teleport**: open any area by its code or from a
   map, shown with a teleport mark on your messages there. A channel room: a plain timeline (not bubbles) with
   nicknames, "Messages here are public and not kept: you see what is said while you are here.", the composer,
   and a people count in the header.
7. **Security settings** (a Settings section): **App lock** (Off, or the device's Face ID / fingerprint /
   passcode on Phone, a local passcode on the web) and its lock screen; **Hide chats in screenshots** (Android
   only); **Erase chats from this device**, a red action with a confirmation: "This removes your chats and their
   keys from this device. Chats can come back from relays only while they are kept there." And a stronger
   **Erase everything** that also removes the account key, with the backup warning from Sign out.
8. **Attachments:** picking an image, the uploading state in the composer ("Encrypting…", then sending), and an
   image message in the conversation.
9. **Errors:** no inbox relays for the recipient, offline (messages wait with Sending… and go when back
   online), and the request from a blocked person not shown.

Also add the Chats tab and its badge to every board that shows the Phone capsule, the rail or the Duo side
column.

## Deliverables of this pass

- Glass app bars and edge-to-edge content (section 1), at rest and scrolled, dark and light, the reduced
  transparency fallback, with a note on the materials and shadow values.
- Chats (section 2): list and Requests, New chat (person and group), conversation with its states, chat info,
  the encryption explainer, verification, location channels (list, first run, room, teleport), security
  settings with app lock and erase, attachments, errors, empty states; for Phone, Web and Duo.
- The updated navigation (Phone capsule with Chats, the rail, the side column).
- Notes with "Actions:" for every new control, and the copy you wrote.
