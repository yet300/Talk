# Talk

A serverless take on [KarpathyTalk](https://karpathytalk.com): a small, positive network for
builders where posts are markdown documents with a Twitter-like social layer (likes, reposts,
quotes, replies, follows). The original site is gone; Talk keeps the idea but stores everything
on **Nostr**, so there is no backend to pay for and every post is open data by design.

Native apps for **Android** (Compose), **iOS** (SwiftUI) and the **web** (React), sharing all
logic through Kotlin Multiplatform.

## Features (parity with KarpathyTalk)

- Sign in with a Nostr key: an external signer (Amber, NIP-07) or a key stored on the device
- Markdown posts (GFM, code blocks, images) up to 10,000 characters
- Replies, quote posts, reposts, likes, bookmarks, follows
- Editable posts with a full revision history
- Feeds: Latest, Following, Trending (Hacker News-style ranking), Activity
- Profiles with Posts and Replies tabs

How each feature maps to Nostr events: [docs/nostr-protocol.md](docs/nostr-protocol.md).

## Project layout

```
app/androidApp    Android app (Compose)
app/iosApp        iOS app (SwiftUI)
app/webApp        Web app (React + Vite)
shared/logic      Shared logic, exported to iOS (SharedLogic.framework) and JS
shared/ui         Android Compose UI
core/common       Dispatchers, logging, Decompose/MVIKotlin helpers
build-logic       Gradle convention plugins
docs              Protocol and research notes
```

Architecture, conventions and commands are in [AGENTS.md](AGENTS.md).

## Running

- **Android:** `./gradlew :androidApp:assembleDebug`, or the run configuration in the IDE.
- **iOS:** open `app/iosApp/iosApp.xcodeproj` in Xcode and run.
- **Web:** install Node.js, then

  ```bash
  npm install
  npm run start
  ```

## Tests

```bash
./gradlew :sharedLogic:testAndroidHostTest :sharedLogic:jsTest :sharedLogic:iosSimulatorArm64Test
```
