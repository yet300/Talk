# .agents/AGENTS.md: skills index

AI agent skills for **Talk**. Project architecture and build rules are in the root
[AGENTS.md](../AGENTS.md). Claude Code reads these skills through the `.claude/skills` symlink.

## Skill index (`.agents/skills/`)

- **Decompose & MVIKotlin** (`decompose-mvikotlin/`)
  - `decompose-component`: ComponentContext, InstanceKeeper, StateKeeper, lifecycle
  - `decompose-navigation`: ChildStack, ChildSlot, ChildPages
  - `decompose-compose`: Compose extensions and UI binding
  - `mvikotlin-code`: StoreFactory, CoroutineExecutor, Reducer, Labels
- **Compose Multiplatform** (`compose-multiplatform/`): state hoisting, deferred reads, side effects,
  stability, animations, adaptive layouts, testing
- **Apple** (`apple/`)
  - `decompose-swiftui`: the Decompose tree rendered with native SwiftUI containers
  - `kotlin-native`: using the `SharedLogic` framework from Swift (SKIE, threading, memory)
  - `iphone-duo`: SwiftUI/UIKit guidelines
- **Android** (`android/`): edge-to-edge, performance, R8
- **Kotlin** (`kotlin/`): control flow, coroutines and structured concurrency, Flow state/events,
  functions, value classes
- **Metro DI** (`metro-di/`): `@DependencyGraph`, `@ContributesTo`, `@ContributesBinding`, KMP wiring
- **KSafe** (`ksafe/`): encrypted key/value storage; this is where the Nostr `nsec` is stored
- **Design** (`design/`): app system, polish, motion
- **Hindsight docs** (`hindsight-docs/`)
