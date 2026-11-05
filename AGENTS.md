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
| `:sharedLogic` | `shared/logic` | Composition root: the Metro graph (`AndroidAppGraph`, `IosAppGraph`) and the umbrella exported to iOS (`SharedLogic.framework`) and JS |
| `:sharedUI` | `shared/ui` | Android Compose UI (Android-only on purpose) |
| `:core:common` | `core/common` | Dispatchers, `Log` (Kermit), Decompose/MVIKotlin helpers, `StoreFactory` binding. Package `com.app.common` |
| `:core:domain` | `core/domain` | Models, repository interfaces, `Pager`, `ContentRules`, `TrendingRanker`, use cases; pure Kotlin |
| `:core:nostr` | `core/nostr` | Protocol only: NIP-01 events and ids, BIP-340 signing (libsecp256k1 on Android/iOS, `@noble/curves` on JS), NIP-19, `RelayClient`/`RelayPool`, NIP helpers |
| `:core:api` | `core/api` | Talk schema (`TalkSchema`, `TalkEvents`, `TalkEvent`) and remote sources over `RelayClient`; Blossom, NIP-05 |
| `:core:database` | `core/database` | SQLDelight cache (`TalkDatabase`), DAO interfaces + implementations, `SqlDriverFactory` per platform |
| `:core:data` | `core/data` | Repository implementations, `Session` + KSafe key vault, `EventIngestor`, `FeedPaging` (Paging 3 + `RemoteMediator`), optimistic `Publisher` |
| `:core:resources` | `core/resources` | moko-resources (`MR`, package `com.yet.talk.resources`) and platform helpers |
| `:feature:root` | `feature/root` | Root `ChildStack`: Splash, Auth, Main and the full-screen destinations above the tabs; `RootStore` swaps Auth/Main on sign-in |
| `:feature:auth` | `feature/auth` | Sign in with a local key (create or import an `nsec`) |
| `:feature:post` | `feature/post` | Two components: `component/post` (post, reply thread, actions) and `component/revisions` |
| `:feature:compose` | `feature/compose` | Editor for `ComposeMode` New / Reply / Quote / Edit, image upload, draft kept by StateKeeper |
| `:feature:profile` | `feature/profile` | A profile (or your own, with `userId = null`): Posts / Replies, follow |
| `:feature:settings` | `feature/settings` | Account, key export, relays (`RelayRules`), sign out |
| `:feature:tab:main` | `feature/tab/main` | Tab container (`ChildStack` + `bringToFront`): Home, Search, Activity, Bookmarks, Profile |
| `:feature:tab:home` | `feature/tab/home` | Home timeline: Following / Popular (trending) / Latest |
| `:feature:tab:search` | `feature/tab/search` | Local search on every keystroke, NIP-50 relays after a 350 ms pause |
| `:feature:tab:activity` | `feature/tab/activity` | Replies, quotes, reposts, likes and follows aimed at you (no store) |
| `:feature:tab:bookmarks` | `feature/tab/bookmarks` | Your NIP-51 bookmarks |

Dependency graph:

```
feature:root → feature:tab:main → feature:tab:{home,search,activity,bookmarks}, feature:profile
feature:root → feature:{auth,post,compose,profile,settings}
every feature → core:domain, core:common
core:data → core:domain, core:database, core:api → core:nostr
sharedLogic → features (api, exported) + core:data (implementation, not exported)
```

Screens follow KarpathyTalk and the Claude Design file "Talk" (X design system). Post titles and
excerpts are derived from the markdown (`PostSummaries`), as in KarpathyTalk.

## Data flow

- **The database is the single source of truth.** Repositories fetch from relays, verify (in `RelayPool`),
  parse (`TalkEvent.parse`), store (`EventIngestor`), and expose `Flow`s read from the DAOs.
- `EventIngestor` enforces authorship: revisions and NIP-09 deletions only count for the author's own events.
- Paging: AndroidX Paging 3 (`paging-common`, KMP). Repositories return `Flow<PagingData<T>>`; the database pages
  through SQLDelight's `QueryPagingSource` (20 rows, `FeedRules.PAGE_SIZE`), and `FeedRemoteMediator` fetches older
  pages from relays. Its cursor (the oldest event relays returned) and end flag live in `sync_state`.
  Plan for the UI phase: on iOS and the web, collect `PagingData` through a `PagingDataPresenter` in the feature layer.
- Writes are optimistic (`Publisher.storeAndPublish`): stored as pending, then published or failed. Deletions
  and unlikes are published first, because tombstones cannot be undone.
- Replaceable lists (contacts, bookmarks, relays) are synced from relays before they are changed
  (`RemoteSync.ensureListsSynced`), and each new version gets a strictly newer `created_at`.
- Errors reach callers as `Result` with a `DomainError`; network failures become `DomainError.Network`.

## Platform notes

- **iOS:** the SQLDelight native driver needs the system SQLite: `OTHER_LDFLAGS = -lsqlite3` in
  `app/iosApp/Configuration/Config.xcconfig`, and `linkerOpts("-lsqlite3")` in Kotlin modules that link native
  test binaries without the SQLDelight plugin (see `core/data/build.gradle.kts`).
- **Web:** SQLite WASM (`@sqlite.org/sqlite-wasm`) in a module worker, persisted in OPFS (SAH pool, no COOP/COEP
  needed; falls back to the plain OPFS VFS, then memory). The worker lives in
  `core/database/src/jsMain/resources/sqlite/talk-sqlite.worker.mjs`; `app/webApp/scripts/copy-sqlite.mjs` copies it
  and the WASM build into `public/sqlite/` before `npm start`/`npm run build`. It answers statements without result
  columns with `db.changes()`, because `WebWorkerDriver` reads the changed-row count from `values[0][0]`. The schema is
  created and migrated by `PRAGMA user_version`. DAO and repository tests skip on JS (Node has no Web Worker/OPFS).
- **Keys:** `KSafeKeyVault` is the only place the secret key is persisted. `SecretKey.toString()` is redacted.

## Components and DI

- **Implementations stay `internal` to their module.** Each module contributes them to the app graph from one
  `di/<Module>Bindings` container (`@ContributesTo(AppScope::class) @BindingContainer abstract class`) with
  `@Binds internal abstract val Impl.bindX: Interface` and, for third-party types, `@Provides` in its companion.
  Containers: `NostrBindings`, `ApiBindings`, `DatabaseBindings`, `DataBindings` + `KeyVaultBindings`,
  `CommonBindings`, and one per feature (`RootBindings`, `AuthBindings`, `PostBindings`, …). Bindings only the
  data layer may reach (the session, the key vault) are `@GraphPrivate`.
- Platform-only inputs (HTTP engine via `relayHttpClient(engine)`, `SqlDriverFactory`, KSafe, Android `Context`)
  are provided by the platform graphs in `sharedLogic` (`AndroidAppGraph`, `IosAppGraph`, `JsAppGraph`).
- Tests that need wiring use a test `@DependencyGraph(AppScope::class, excludes = [...])` with fakes as factory
  inputs (see `core/data/src/commonTest/.../fake/DataTestGraph.kt`).
- Each component is `interface XxxComponent` (with a nested `fun interface Factory`) +
  `internal class DefaultXxxComponent` (`ComponentContext by componentContext`) +
  `internal class DefaultXxxComponentFactory` (`@Inject`), bound in the feature's bindings container.
- A parent receives its children's `Factory` types through its own factory's constructor; Metro
  wires the chain. Nothing calls a `Default…` constructor outside its own module.
- State is exposed as Decompose `Value`; navigation configs are private `@Serializable` types.
- Feature layout (as in TetrisLite `feature/history`): `XComponent`, `DefaultXComponent`, `PreviewXComponent`,
  `store/XStore` + `store/XStoreFactory` (MVIKotlin, created with `instanceKeeper.getStore`), `mapper/XStateMapper`
  (`State → Model`), `di/XBindings`. A module with several components puts each in `component/<name>/` with its
  own `store/` and `mapper/`.
- Navigation leaves a component through a single `Output` callback in its factory (or `onBack` when that is all);
  the parent maps child outputs to its own. Paged lists are `Flow<PagingData<T>>` on the component (`cachedIn` its
  scope), not in the store. A component with no state of its own has no store (Activity).
- Store tests use `DefaultStoreFactory`, `isAssertOnMainThreadEnabled = false` and `Dispatchers.setMain(...)`
  with hand-written fakes of the domain interfaces.
- The platform apps get the graph from `createAndroidAppGraph()` / `IosAppGraphKt.createIosAppGraph()`
  and create the root with `graph.rootComponentFactory(componentContext)` on the main thread.

KMP targets: Android, `iosArm64`, `iosSimulatorArm64`, `js` (browser library; tests run on Node).

## Build conventions (`build-logic/convention`)

| Plugin alias | What it does |
|---|---|
| `local.kotlin.multiplatform` | KMP + Android library + serialization; all targets; JDK 17; coroutines, datetime and serialization in `commonMain`; the `testing` bundle in `commonTest`. The Android namespace is derived from the path (`:core:common` → `com.yet.common`) |
| `local.compose.multiplatform` | Compose plugin + compiler; runtime, foundation, ui, material3, adaptive |
| `local.feature` | KMP + Metro; `api(:core:domain)`, `:core:common`, Decompose and MVIKotlin bundles; namespace `com.yet.talk.feature.…` |
| `local.sqlDelight` | SQLDelight with `generateAsync = true` (required by the JS worker driver); Android, native and web-worker drivers |

New modules use these plugins. Add every dependency to `gradle/libs.versions.toml` first; never put
versions in build files. Reference modules with typesafe accessors (`implementation(projects.core.domain)`),
never `project(":…")`. The one exception is convention plugins in `build-logic`, where the accessors do not exist.

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
- After `kotlinUpgradeYarnLock` is needed, run it on its own before other Gradle tasks in the same invocation.
- Live relay checks are opt-in and read-only: `TALK_LIVE_RELAYS=1 ./gradlew :core:nostr:testAndroidHostTest --tests '*LiveRelayTest*'`.
- Skills for this stack are in `.agents/skills/` (index: `.agents/AGENTS.md`). Use them before
  writing Decompose, MVIKotlin, Metro, Compose, SwiftUI-interop or KSafe code.
