# AGENTS.md: Talk

Talk is a serverless clone of [KarpathyTalk](https://karpathytalk.com) (Twitter meets GitHub Gists:
markdown posts plus likes, reposts, quotes, replies and follows), built on **Nostr**. There is no
backend of our own: public relays store the data and the apps are the only code we run.

- Product reference: the original Go source in `../KarpathyTalkApp/reference/KarpathyTalk-master`
  (`internal/app/app.go` lists every route; `docs.md` documents the data model).
- Protocol contract: [docs/nostr-protocol.md](docs/nostr-protocol.md). Read it before touching
  anything that creates or reads events.
- UI contract: [docs/design/ui-review.md](docs/design/ui-review.md): every screen, control, size class and state
  of the Claude Design file "Talk", plus the decisions on what it leaves open. Build only what it lists.
- Library research: [docs/research/nostr-crypto-web.md](docs/research/nostr-crypto-web.md).

## Principles

Clean Architecture, SOLID, KISS, YAGNI. In practice:

- Build only what a KarpathyTalk feature needs today. No speculative abstractions, NIPs or targets.
- Dependencies point inward: `feature → core:domain ← data`. Features never see data or Nostr types.
- Native UI per platform: Compose on Android, SwiftUI on iOS, Compose HTML (DOM, not canvas) on the web. All
  logic below the UI (components, stores, use cases, repositories) is shared Kotlin.
- One responsibility per module; a module exposes interfaces, not its implementation details.

## Platforms and modules

| Gradle path | Directory | Role |
|---|---|---|
| `:androidApp` | `app/androidApp` | Android entry point: `TalkApplication` owns the graph, `MainActivity` the retained root |
| — | `app/iosApp` | iOS app (SwiftUI); links the `SharedLogic` framework. `AppDelegate` owns the graph and root; `Theme/`, `Decompose/` |
| `:webApp` | `app/webApp` | Web app (Compose HTML, webpack): `Main.kt` wires the graph, `withWebHistory` and the theme |
| `:sharedLogic` | `shared/logic` | Composition root: the Metro graph (`AndroidAppGraph`, `IosAppGraph`, `JsAppGraph`), `DomainError.toStringDesc()`, the umbrella exported to iOS (`SharedLogic.framework`) |
| `:sharedUI` | `shared/ui` | Android Compose UI (Android-only on purpose, code in `androidMain`): `TalkTheme`, root content |
| `:core:common` | `core/common` | Dispatchers, `Log` (Kermit), Decompose/MVIKotlin helpers, `StoreFactory` binding. Package `com.app.common` |
| `:core:domain` | `core/domain` | Models, repository interfaces, `Pager`, `ContentRules`, `TrendingRanker`, use cases; pure Kotlin |
| `:core:nostr` | `core/nostr` | Protocol only: NIP-01 events and ids, BIP-340 signing (libsecp256k1 on Android/iOS, `@noble/curves` on JS), NIP-19, `RelayClient`/`RelayPool`, NIP helpers |
| `:core:api` | `core/api` | Talk schema (`TalkSchema`, `TalkEvents`, `TalkEvent`) and remote sources over `RelayClient`; Blossom, NIP-05 |
| `:core:database` | `core/database` | SQLDelight cache (`TalkDatabase`), DAO interfaces + implementations, `SqlDriverFactory` per platform |
| `:core:data` | `core/data` | Repository implementations, `Session` + KSafe key vault, `EventIngestor`, `FeedPaging` (Paging 3 + `RemoteMediator`), optimistic `Publisher` |
| `:core:resources` | `core/resources` | moko-resources (`MR`, package `com.yet.talk.resources`): strings, plurals, light/dark colours, icons; `TalkTokens` (spacing, radii, sizes, type scale) |
| `:feature:root` | `feature/root` | Root `ChildStack`: Splash, Auth, Main, and Compose, Settings, Revisions above it; `RootStore` swaps Auth/Main on sign-in and exposes the theme |
| `:feature:auth` | `feature/auth` | Sign in with a local key (create or import an `nsec`) |
| `:feature:post` | `feature/post` | Two components: `component/post` (post, reply thread, actions) and `component/revisions` |
| `:feature:compose` | `feature/compose` | Editor for `ComposeMode` New / Reply / Quote / Edit, image upload, draft kept by StateKeeper |
| `:feature:profile` | `feature/profile` | A profile (or your own, with `userId = null`): Posts / Replies, follow |
| `:feature:settings` | `feature/settings` | Account, key export, relays (`RelayRules`), sign out |
| `:feature:tab:main` | `feature/tab/main` | `MainComponent`: list-detail `childPanels` — `component/tabs` (Home, Search, Activity, Bookmarks, Profile) in the main panel, `component/details` (a stack of posts and profiles) in the details panel; `isReading` for the Duo replies pose |
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
androidApp → sharedUI → sharedLogic;  webApp → sharedLogic;  iosApp → SharedLogic.framework
```

Screens follow KarpathyTalk and the Claude Design file "Talk" (X design system). Post titles and
excerpts are derived from the markdown (`PostSummaries`), as in KarpathyTalk.

## Data flow

- **The database is the single source of truth.** Repositories fetch from relays, verify (in `RelayPool`),
  parse (`TalkEvent.parse`), store (`EventIngestor`), and expose `Flow`s read from the DAOs.
- `EventIngestor` enforces authorship: revisions and NIP-09 deletions only count for the author's own events.
- Paging: AndroidX Paging 3 (`paging-common`, KMP). Repositories return `Flow<PagingData<T>>`; the database pages
  through `OffsetPagingSource` (20 rows, `FeedRules.PAGE_SIZE`; SQLDelight's own `QueryPagingSource` reads
  synchronously, which the web worker driver rejects), and `FeedRemoteMediator` fetches older
  pages from relays. Its cursor (the oldest event relays returned) and end flag live in `sync_state`.
  The web collects `PagingData` with a `PagingDataPresenter` (`app/webApp/.../paging`); iOS will do the same in Swift-facing Kotlin.
- Writes are optimistic (`Publisher.storeAndPublish`): stored as pending, then published or failed. Deletions
  and unlikes are published first, because tombstones cannot be undone.
- Replaceable lists (contacts, bookmarks, relays) are synced from relays before they are changed
  (`RemoteSync.ensureListsSynced`), and each new version gets a strictly newer `created_at`.
- Errors reach callers as `Result` with a `DomainError`; network failures become `DomainError.Network`.

## Platform notes

- **iOS:** the SQLDelight native driver needs the system SQLite: `OTHER_LDFLAGS = -lsqlite3` in
  `app/iosApp/Configuration/Config.xcconfig`, and `linkerOpts("-lsqlite3")` in Kotlin modules that link native
  test binaries without the SQLDelight plugin (see `core/data/build.gradle.kts`).
- **iOS resources:** `SharedLogic` is a static framework, so the Xcode build phase also runs
  `:sharedLogic:copyFrameworkResourcesToApp` (moko copies `Talk.core:resources.bundle` into the app). In Swift,
  qualify `SharedLogic.ColorResource` / `SharedLogic.ImageResource`: Xcode's asset symbols use the same names.
- **Web:** SQLite WASM (`@sqlite.org/sqlite-wasm`, an npm dependency of `:webApp`) in a module worker, persisted in
  OPFS (SAH pool, no COOP/COEP needed; falls back to the plain OPFS VFS, then memory). The worker lives in
  `core/database/src/jsMain/resources/sqlite/talk-sqlite.worker.mjs`; the `copySqliteWasm` task of `:webApp` serves
  it and the WASM build at `/sqlite/` (`<base href="/">` keeps that path absolute under routes like `/post/<id>`).
  It answers statements without result
  columns with `db.changes()`, because `WebWorkerDriver` reads the changed-row count from `values[0][0]`. The schema is
  created and migrated by `PRAGMA user_version`. DAO and repository tests skip on JS (Node has no Web Worker/OPFS).
- **Web routing:** Decompose `withWebHistory`; the root and `MainComponent` are `WebNavigationOwner`s (paths:
  `/signin`, `/post/<id>`, `/post/<id>/revisions`, `/user/<id>`, `/settings`, `/compose`, tabs `/search`,
  `/activity`, `/bookmarks`, `/me`). A shared link is the root factory's `deepLink`, opened above Main after sign-in.
  The dev server falls back to `index.html` (`webpack.config.d/devServer.js`).
- **Keys:** `KSafeKeyVault` is the only place the secret key is persisted. `SecretKey.toString()` is redacted.
  Android backups are off (`allowBackup="false"`).

## Web UI (`app/webApp`, Compose HTML)

- Layout: `web/ui/` with `theme/` (tokens as CSS variables, strings, icons), `components/` (shared: buttons,
  tabs, top bar, post card, paged list, markdown, menus and dialogs), `root/` (the root stack, the rail and the
  page shell) and `screens/<screen>/` with `XContent.kt` plus `component/`, `dialog/`, `sheet/` for parts only
  that screen uses (`screens/tab/<tab>/` for the tabs). Every platform's UI follows this layout.
- Styles are the design's own CSS in `resources/styles/` (`x-components.css` from the X design system,
  `talk.css` for layout and parts, `app.css` for what the design does not spell out); composables set its
  class names. `TalkCss` writes the tokens under the design's variable names (`--x-blue`, `--space-lg`…).
- Paging: `paging/PagingItems` (a `PagingDataPresenter`); `PagedList` loads the next page when its end
  scrolls into view. Markdown: `marked` + `DOMPurify` (sanitized before `innerHTML`).
- The web is always one pane: an open post covers the tabs, its replies sit in the right column (under the
  article below 1018 px).

## Resources and theme

- One source for every platform: `core:resources` holds strings (`base/strings.xml`, `plurals.xml`), colours
  (`colors/colors.xml`, `<light>`/`<dark>`, RRGGBBAA) and icons (`images/ic_*.svg`, the design's 24×24 stroke set,
  tinted at use); `TalkTokens` holds the non-colour tokens. Values come from the Claude Design file "Talk"
  (X design system `tokens.json`).
- moko generates each resource as an extension property: import `com.yet.talk.resources.*` next to `MR`.
- **Android:** `TalkTheme` maps the tokens onto Material 3 and adds `MaterialTheme.talkColors`, `.talkTypography`,
  `.spacing`, `.sizes`, `.talkShapes`; `TalkIcon`, `TalkIcons`, `stringResource(MR.strings.x)`, `DomainError.message()`.
- **iOS:** `TalkColor`, `TalkSpacing`, `TalkRadius`, `TalkSize`, `.talkTextStyle(_:)`, `TalkIcon`/`TalkIcons`,
  `MR.strings.shared.x.text`, `DomainError.text`.
- **Web:** `TalkCss` writes the tokens as CSS variables (`prefers-color-scheme`, or `data-theme` on `<html>`);
  `TalkColor`, `TalkSpacing`, `TalkRadius`, `StyleScope.textStyle(...)`, `TalkIcon` (CSS mask), `stringResource`.

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
- Large screens: `MainComponent` uses Decompose `childPanels`; the UI sets the mode (`onModeChanged`) from the
  window: SINGLE on phones, the Duo outer screen and the web, DUAL on the Duo inner screen sideways.
- Navigation leaves a component through a single `Output` callback in its factory (or `onBack` when that is all);
  the parent maps child outputs to its own. Paged lists are `Flow<PagingData<T>>` on the component (`cachedIn` its
  scope), not in the store. A component with no state of its own has no store (Activity).
- Store tests use `DefaultStoreFactory`, `isAssertOnMainThreadEnabled = false` and `Dispatchers.setMain(...)`
  with hand-written fakes of the domain interfaces.
- The platform apps get the graph from `createAndroidAppGraph()` / `IosAppGraphKt.createIosAppGraph()` /
  `createJsAppGraph()` and create the root with `graph.rootComponentFactory(componentContext, deepLink)` on the main
  thread (Android: `retainedComponent`; iOS: `ApplicationLifecycle` + a `BackDispatcher`; web: `withWebHistory`).
- `RootComponent` is a `BackHandlerOwner` (predictive back on Android via `predictiveBackAnimation`, swipe back on
  iOS) and, with `MainComponent`, a `WebNavigationOwner`.

KMP targets: Android, `iosArm64`, `iosSimulatorArm64`, `js` (browser; tests run on Node).

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
./gradlew :webApp:jsBrowserDevelopmentRun --continuous     # web app at http://localhost:8080
./gradlew :webApp:jsBrowserDistribution                     # production web bundle (build/dist/js)
./gradlew :sharedLogic:testAndroidHostTest :sharedLogic:jsTest :sharedLogic:iosSimulatorArm64Test
./gradlew kotlinUpgradeYarnLock                             # after npm dependencies change
```

iOS: open `app/iosApp/iosApp.xcodeproj`; the build phase runs `:sharedLogic:embedAndSignAppleFrameworkForXcode`
and `:sharedLogic:copyFrameworkResourcesToApp`.

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
