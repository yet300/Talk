# Chats: research and decisions (2026-11-07)

Talk gets private chats like X's Chat, with bitchat's Nostr features, and without X Chat's security
weaknesses. Bluetooth mesh is out of scope. This note records what the originals do, what Talk takes from
each, and the protocol Talk will use. Nothing here is built yet; the design pass comes first
([claude-design-prompt-3.md](../design/claude-design-prompt-3.md)).

## 1. X Chat (XChat), the product reference

A standalone app since 24 April 2026 (iOS 26+), and the Messages tab in X.

- **Features:** 1:1 and group chats (reports say 350 to 481 people), audio and video calls, files, large photos and
  videos, disappearing messages (down to 5 minutes), screenshot blocking or alerts, message requests from people
  you do not follow, reactions, replies, edit and delete.
- **Security, as analysed by Matthew Garrett and Mysk:**
  - Private keys are escrowed on X's servers through the Juicebox protocol, protected by a **4-digit PIN**.
    Juicebox can split key shares across independent custodians and accept long PINs; X uses neither, and runs the
    custodians itself, in software rather than HSMs.
  - Long-term keys, so **no forward secrecy and no post-compromise security**.
  - Metadata (who talks to whom, when) is fully visible to X.

Sources: [CyberInsider: XChat's E2EE critically weak](https://cyberinsider.com/xchats-end-to-end-encryption-critically-weak-warns-researcher/),
[CyberInsider: standalone app, concerns remain](https://cyberinsider.com/xchat-launches-standalone-ios-app-as-security-concerns-remain/),
[Mysk on X](https://x.com/mysk_co/status/2047807880124911701), [Atomic Mail overview](https://atomicmail.io/blog/xchat-by-elon-musk-overview-privacy-claims-vs-reality),
[TechCrunch: XChat app](https://techcrunch.com/2026/04/24/x-launches-xchat-app-ios-messaging-iphone/),
[MacRumors](https://www.macrumors.com/2026/04/24/xchat-app-now-available/).

## 2. bitchat, the Nostr feature reference

[bitchat](https://github.com/permissionlesstech/bitchat) ([Android](https://github.com/permissionlesstech/bitchat-android),
[whitepaper](https://github.com/permissionlesstech/bitchat/blob/main/WHITEPAPER.md)): no accounts, no phone
numbers, no servers. Its Nostr side, which Talk can use:

- **Location channels:** public rooms per geohash, at five levels: region (2 characters), province (4), city (5),
  neighborhood (6), block (7). Messages are ephemeral Nostr events **kind 20000** with `["g", geohash]`, an
  optional nickname `["n", name]`, and `["t", "teleport"]` when the sender joined a place they are not in. Each
  geohash gets its **own throwaway key**, so the user's identity is not linked across places.
- **Presence:** **kind 20001** heartbeats every 40 to 80 seconds with only `["g", geohash]`, sent **only at
  region, province and city precision**; finer channels show "? people" instead of a count.
  ([GeohashPresenceSpec](https://github.com/permissionlesstech/bitchat-android/blob/main/docs/GeohashPresenceSpec.md))
- **Optional proof of work** (NIP-13) on channel messages, against spam.
- **Private messages over Nostr:** reuse NIP-17/NIP-59 kind numbers but with a **proprietary encryption**, so they
  only work between bitchat clients. No forward secrecy (the whitepaper says so).
- **Favorites** (mutual trust unlocks private messages), **QR verification** of a contact in person,
  **panic wipe** (triple tap clears keys and data), IRC-style `/commands`, **ephemeral timelines** (chats in
  memory only).

## 3. Decisions for Talk

**Private chats use the standard Nostr DMs, not bitchat's envelope:** NIP-17 messages (kind 14 text, 15 file,
7 reaction) sealed (NIP-59 kind 13) and gift-wrapped (kind 1059) with NIP-44 v2 encryption, to each recipient
and to the sender. They work with other Nostr apps (0xchat, Amethyst, Coracle, …).

- **Inbox relays:** messages go only to the recipient's kind 10050 list. Talk publishes the viewer's own 10050
  (1 to 3 relays, preferably ones that enforce NIP-42 AUTH so only the recipient can download their wraps). No
  10050: "This person can't receive chats yet."
- **Metadata:** relays see that someone wrote to the recipient (the wrap's `p` tag), not who, what or when (the
  seal and wrap timestamps are randomized up to two days back).
- **Groups:** NIP-17 groups, **up to 10 people** (each message is wrapped once per member). No admins, as NIP-17
  says. Larger groups with forward secrecy would use the Marmot protocol (MLS over Nostr, kinds 443 to 445):
  later, not in this pass.
- **Attachments:** kind 15 file messages; the file is encrypted (AES-GCM) before it goes to Blossom, and the key
  and nonce travel inside the encrypted message. Images up to 5 MB, as in posts.
- **Replies and reactions:** yes (an `e` tag; kind 7 inside the wrap). **Delete for everyone:** a wrapped
  deletion, best effort ("Other apps may still show it"). **Edit:** no.
- **No read receipts and no typing indicators:** they leak activity; NIP-17 defines none.
- **No calls:** they need servers (TURN, signalling) that Talk does not run.

**Security model (where X Chat fails):**

- **No PIN and no key escrow.** The key is the user's `nsec`, kept only in KSafe on the device. Restoring chats on a
  new device means importing the `nsec`; relays still hold the wraps.
- **Honest about forward secrecy:** NIP-17 has none, like bitchat's envelopes. The chat info explains it in one
  line; **disappearing messages** (an NIP-40 `expiration` on each wrap, and deletion on the device) are the
  mitigation, as NIP-17 suggests.
- **Chats encrypted at rest:** message text and attachment keys are stored encrypted in the database with a key
  kept in KSafe.
- **App lock:** opening Chats can require the device's biometrics or passcode (Android, iOS) or a local passcode
  (web). It protects the device; it is not a key escrow.
- **Panic wipe** (from bitchat): "Erase chats from this device" removes messages, attachments and the chat key;
  a stronger version also removes the account key, after showing the backup warning.
- **Verify a contact:** compare a short code (from both public keys) or scan a QR code in person; a verified
  contact shows a check.
- **Requests:** chats from people you do not follow land in Requests (Accept, Delete, Block). **Block and mute:**
  NIP-51 mute list (kind 10000), private entries encrypted to yourself.
- **Screenshots:** blocking is possible only on Android (`FLAG_SECURE`), so it is an Android-only setting; Talk
  never promises it.

**Location channels, compatible with bitchat:** kinds 20000 and 20001 with the same tags, so Talk users and
bitchat users meet in the same rooms. The geohash is computed on the device (the location never leaves it);
the user picks the level; each channel uses its own throwaway key with a chosen nickname; presence only at
region, province and city; channel messages are not stored by relays (ephemeral), so a room shows what arrived
while it was open. Teleport lets the user open any geohash. Optional proof of work.

**Platform work this needs:** NIP-44 v2 (ChaCha20, HKDF-SHA256, HMAC-SHA256: `@noble/ciphers` on the web; a
KMP implementation or libsodium on Android and iOS), NIP-59 sealing, AES-GCM for files (WebCrypto,
`javax.crypto`, CryptoKit), geohash encoding, NIP-13 proof of work.
