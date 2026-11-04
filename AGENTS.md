# AGENTS.md: Talk

Talk is a serverless clone of [KarpathyTalk](https://karpathytalk.com) (Twitter meets GitHub Gists:
markdown posts plus likes, reposts, quotes, replies and follows), built on **Nostr**. There is no
backend of our own: public relays store the data and the apps are the only code we run.

- Product reference: the original Go source in `../KarpathyTalkApp/reference/KarpathyTalk-master`
  (`internal/app/app.go` lists every route; `docs.md` documents the data model).
- Protocol contract: [docs/nostr-protocol.md](docs/nostr-protocol.md). Read it before touching
  anything that creates or reads events.
- Library research: [docs/research/nostr-crypto-web.md](docs/research/nostr-crypto-web.md).

## Principles

Clean Architecture, SOLID, KISS, YAGNI. In practice:

- Build only what a KarpathyTalk feature needs today. No speculative abstractions, NIPs or targets.
- Dependencies point inward: `feature → core:domain ← data`. Features never see data or Nostr types.
- Native UI per platform: Compose on Android, SwiftUI on iOS, React on web. All logic below the UI
  (components, stores, use cases, repositories) is shared Kotlin.
- One responsibility per module; a module exposes interfaces, not its implementation details.

## Platforms and modules

| Gradle path | Directory | Role |
|---|---|---|
| `:androidApp` | `app/androidApp` | Android entry point (Compose) |
| — | `app/iosApp` | iOS app (SwiftUI); links the `SharedLogic` framework |
| — | `app/webApp` | Web app (React + Vite); consumes the `sharedLogic` JS library |
| `:sharedLogic` | `shared/logic` | Umbrella module exported to iOS and JS; the root Metro graph and the root component |
| `:sharedUI` | `shared/ui` | Android Compose UI (Android-only on purpose) |
| `:core:common` | `core/common` | Dispatchers, `Log` (Kermit), Decompose/MVIKotlin helpers, `StoreFactory` binding. Package `com.app.common` |
| `:core:domain` | planned | Entities, repository interfaces, use cases; pure Kotlin |
| `:core:nostr` | planned | NIP-01 event model, relay pool (Ktor WebSockets), `Signer`, bech32 |
| `:feature:*` | planned | Decompose components + MVIKotlin stores, no UI (`local.feature` plugin) |

KMP targets: Android, `iosArm64`, `iosSimulatorArm64`, `js` (browser library; tests run on Node).

## Build conventions (`build-logic/convention`)

| Plugin alias | What it does |
|---|---|
| `local.kotlin.multiplatform` | KMP + Android library + serialization; all targets; JDK 17; coroutines, datetime and serialization in `commonMain`; the `testing` bundle in `commonTest`. The Android namespace is derived from the path (`:core:common` → `com.yet.common`) |
| `local.compose.multiplatform` | Compose plugin + compiler; runtime, foundation, ui, material3, adaptive |
| `local.feature` | KMP + Metro; `api(:core:domain)`, `:core:common`, Decompose and MVIKotlin bundles; namespace `com.yet.talk.feature.…` |
| `local.sqlDelight` | SQLDelight with `generateAsync = true` (required by the JS worker driver); Android, native and web-worker drivers |

New modules use these plugins. Add every dependency to `gradle/libs.versions.toml` first; never put
versions in build files.

## Stack

Kotlin 2.4 · Compose Multiplatform · Decompose + Essenty · MVIKotlin · Metro DI · SKIE (iOS) ·
kotlinx (coroutines, serialization, datetime) · Ktor client + WebSockets · SQLDelight (local
cache) · KSafe (key storage) · Kermit · moko-resources. Tests: kotlin-test, coroutines-test,
Turbine, Mokkery.

## Commands

```bash
./gradlew :androidApp:assembleDebug                         # Android app
./gradlew :sharedLogic:compileKotlinIosSimulatorArm64       # iOS compile check
./gradlew :sharedLogic:jsBrowserDevelopmentLibraryDistribution  # JS library for the web app
./gradlew :sharedLogic:testAndroidHostTest :sharedLogic:jsTest :sharedLogic:iosSimulatorArm64Test
./gradlew kotlinUpgradeYarnLock                             # after npm dependencies change
npm run start                                               # web app (builds sharedLogic first)
```

iOS: open `app/iosApp/iosApp.xcodeproj`; the build phase runs `:sharedLogic:embedAndSignAppleFrameworkForXcode`.

## Rules for agents

- Verify a change with the narrowest Gradle task that covers it; run the iOS and JS compile tasks too
  when you change `commonMain`.
- Keep the Nostr schema in `docs/nostr-protocol.md` and the code in sync; events already on relays
  cannot be migrated.
- Never log, print or persist private keys (`nsec`) outside KSafe.
- Skills for this stack are in `.agents/skills/` (index: `.agents/AGENTS.md`). Use them before
  writing Decompose, MVIKotlin, Metro, Compose, SwiftUI-interop or KSafe code.
