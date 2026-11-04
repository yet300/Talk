---
name: kotlin-native
description: Use the Kotlin/Native SharedLogic framework from Swift in Thaler — how it is built and linked, how Kotlin types look in Swift through SKIE, threading, memory, money and dates, resources, and the rebuild loop. Use when writing or debugging Swift that calls Kotlin, or when a Kotlin API is awkward from Swift.
---

# Kotlin/Native from Swift (Thaler)

The Apple apps link one Kotlin framework, **`SharedLogic`**, built by `:sharedLogic`. It carries the component tree
(Decompose + MVIKotlin), the domain types, the resources (`:core:resources`, moko) and the app graph (Metro). Swift
never sees `core:data` or `core:database`.

## Build and link

- **Static framework** `SharedLogic` (`isStatic = true`), exported modules: `core:domain`, `core:resources`, every
  feature module, Decompose/Essenty, moko `resources` and `graphics` (see `sharedLogic/build.gradle.kts`). A type
  from a non-exported module shows up in Swift only as an opaque name — export the module rather than wrapping.
- Xcode build phases, in order:
  1. **Compile Kotlin Framework** → `./gradlew :sharedLogic:embedAndSignAppleFrameworkForXcode`;
  2. Swift sources;
  3. **Copy Kotlin Resources** → `:sharedLogic:copyFrameworkResourcesToApp` (moko bundle with strings, colours,
     fonts, images; a static framework can't carry it). Without this phase every `MR` lookup returns empty/default.
- `ENABLE_USER_SCRIPT_SANDBOXING = NO` is required for both script phases.
- Keep Xcode's DerivedData **outside** the repo's Gradle `build/` folders: Gradle's stale-output cleanup empties them.
- After changing Kotlin, rebuild from Xcode (the phase runs Gradle) or run
  `./gradlew :sharedLogic:linkDebugFrameworkIosSimulatorArm64` to see Kotlin errors faster.
- `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES` skips the Gradle phase (IDE indexing). Don't set it for real builds.
- If the Xcode project changes on disk while Xcode has it open, Xcode may write its in-memory copy back and drop
  build phases. Close the project before pulling or merging changes to `project.pbxproj`.

## How Kotlin looks in Swift (with SKIE 0.10.15)

| Kotlin                                     | Swift                                                                                                                                                     |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `class`, `data class`                      | class; `data class` gets `==`/`hash` and `doCopy(...)`                                                                                                    |
| `interface`                                | protocol (generic parameters of interfaces are lost — use them through classes such as `Value<T>`)                                                        |
| `object Foo`                               | `Foo.shared`                                                                                                                                              |
| `companion object`                         | `Foo.companion` (or `Foo.Companion.shared`)                                                                                                               |
| `sealed class/interface`                   | class hierarchy; **`switch onEnum(of: x) { case .a(let a): … }`** — exhaustive, no `default`                                                              |
| `enum class`                               | real Swift `enum` (SKIE), exhaustive `switch`                                                                                                             |
| top-level `fun f()` in `File.kt`           | `FileKt.f()`                                                                                                                                              |
| extension `fun T.f()`                      | method on `T` when `T` is a Kotlin class; otherwise `FileKt.f(receiver)`                                                                                  |
| `Int`, `Long`, `Boolean`, `Double`         | `Int32`, `Int64`, `Bool`, `Double`; inside generics and nullable: `KotlinInt`, `KotlinLong`, `KotlinBoolean` (`.int32Value`, `.int64Value`, `.boolValue`) |
| `String?`, `List<T>`, `Map<K,V>`, `Set<T>` | `String?`, `[T]`, `[K: V]`, `Set<T>`                                                                                                                      |
| `suspend fun`                              | `async throws` (SKIE); cancelling the Swift `Task` cancels the coroutine                                                                                  |
| `Flow<T>` / `StateFlow<T>`                 | `AsyncSequence` (`for await x in flow`) / with `.value`                                                                                                   |
| default arguments                          | **not** exported: pass every argument from Swift, or add a Kotlin overload                                                                                |
| `@Throws`                                  | `throws`; any other Kotlin exception crashes the app                                                                                                      |

Collision names get prefixes: `SharedLogic.ColorResource` vs iOS 17's `ColorResource`, moko-graphics `Color` vs
`SwiftUI.Color`. Qualify with the module name.

## Rules

- **Components are the API.** Swift reads `component.model` (a Decompose `Value`) and calls `component.onXxx(...)`.
  It never calls repositories, commands or stores, and never keeps its own copy of business state.
- **Main thread only.** MVIKotlin asserts the main thread; Decompose `Value` emits on main. Call component methods
  from SwiftUI actions/`@MainActor` code; never from a background `Task` without hopping to the main actor.
- **No exceptions across the bridge.** Component calls don't throw by contract. If Swift needs a fallible Kotlin call,
  add `@Throws` or (better) a result type on the Kotlin side.
- **Memory.** Kotlin/Native has a tracing GC; Swift ↔ Kotlin references are fine. Cancel every `Value.subscribe`
  (`Cancellation.cancel()`) when the observer goes away — `ObservableValue` already does. Closures stored in Kotlin
  that capture a SwiftUI view model should capture `[weak self]`.
- **Money:** `Money` is minor units (`Long` → `Int64`) plus `CurrencySpec` (code, decimal places). Format with
  `Decimal(minor) / pow(10, decimals)` and Foundation's currency formatting for the user's locale. Never go through
  `Double`. Totals are per currency — never add two currencies.
- **Dates:** kotlinx-datetime `LocalDate`/`YearMonth` arrive as Kotlin classes. Convert with `year`, `month`/
  `monthNumber`, `day` into `DateComponents` and format with `Date.FormatStyle` in the user's calendar and locale.
  Send dates back to Kotlin as `LocalDate(year:month:day:)` (add a Kotlin helper if a constructor is awkward).
  "Today" is decided by the model (profile time zone), not by `Date()` in Swift.
- **Text:** UI copy comes from `MR.strings` (`Text(resource: \.key)`), domain labels from Kotlin `StringDesc`
  (`Text(desc: …)`). See `app/iosApp/iosApp/Resources/ThalerResources.swift`. No hard-coded user-facing strings.
- **Colours, fonts, tokens** come from `:core:resources`: `Color(resource:)`, `AccentPalette`, `ThalerType`
  (`.thalerText(style)`), `ThalerSpacing`, `ThalerRadius`, `ThalerMotion`. No hard-coded accent green anywhere.

## Kotlin-side fixes are allowed

If an API is clumsy from Swift (a default argument, an `Int?` in a generic, a `LocalDate` constructor), prefer a small
Swift-friendly addition in Kotlin (`sharedLogic` `appleMain` or the feature's public API) over a Swift workaround. Keep
it in Kotlin's terms (no UI types) and add a test.

## Debugging

- "Undefined symbol" / missing type → module not exported, or the framework wasn't rebuilt for the current
  destination (simulator vs device vs Mac).
- Empty strings, black colours, system font → the Copy Kotlin Resources phase didn't run.
- Crash with a Kotlin stack trace → an exception crossed the bridge or a store was touched off the main thread.
- Swift sees `Any`/`AnyObject` where a type was expected → a generic interface lost its parameter; cast through the
  sealed child (`onEnum(of:)`) or expose a concrete class.
