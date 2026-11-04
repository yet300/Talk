---
name: decompose-swiftui
description: Render Talk's Decompose component tree in SwiftUI with native containers — observing Value, ChildStack as NavigationStack, ChildPanels as NavigationSplitView or a stack, ChildSlot as sheets and dialogs, the main tabs as a Liquid Glass TabView, back and swipe-back, lifecycle, state restoration and previews. Use when building or changing any Apple screen.
---

# Decompose in SwiftUI (Talk)

Navigation and state live in Kotlin components (ADR-028). SwiftUI **renders** them: every view takes its component,
observes `Value`s, and forwards user actions to `component.onXxx(...)`. A view never decides where to navigate on its
own and never pops a stack by itself — it tells the component, and the component's new state redraws the view.

Read with: `.agents/skills/apple/kotlin-native/SKILL.md` (types, threading), `.agents/skills/decompose-mvikotlin/`
(Kotlin side), `.agents/skills/apple/iphone-duo/` (fold and sizes).

## Observing a `Value`

Use the helpers in `app/iosApp/iosApp/DecomposeHelpers/`:

```swift
struct HomeDashboardView: View {
    let component: HomeDashboardComponent
    @StateValue private var model: HomeDashboardComponentModel

    init(_ component: HomeDashboardComponent) {
        self.component = component
        _model = StateValue(component.model)
    }
}
```

- One `@StateValue` per `Value` the view needs (`model`, `stack`, `sheet`, `panels`…).
- A child view gets the **child component**, not the parent's model.
- Don't copy model fields into `@State`. Local `@State` is only for pure UI state the component doesn't own
  (focus, scroll position, a text field's in-progress selection, animation phases).

## Containers

| Kotlin                                                                  | SwiftUI                                                                                                                         |
|-------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `ChildStack` with back (Onboarding, Accounts, Budget)                   | `NavigationStack(path:)`; the path is `stack.items.dropFirst()`; the setter calls `onBackClicked(toIndex:)` — see `StackView`   |
| `ChildStack` without back (Root: Loading/StorageFailed/Onboarding/Main) | a `switch onEnum(of: stack.active.instance)` with a transition; no navigation container                                         |
| `MainComponent.tabs` (bringToFront)                                     | `TabView(selection:)` — see "Tabs"                                                                                              |
| `ChildPanels` (Home: main/details/extra; History: list/details)         | regular width: `NavigationSplitView` (2 or 3 columns); compact: `NavigationStack` whose path is [details, extra] — see "Panels" |
| `ChildSlot` sheet (Entry, Accounts.sheet, Budget.sheet, pickers…)       | `.sheet(item:)` with detents, or `.fullScreenCover` where the design says so; dismissing calls the component's dismiss/close    |
| `ChildSlot` dialog (`Entry.dialog` discard)                             | `.confirmationDialog` / `.alert`                                                                                                |
| `Settings.detail` slot                                                  | a pushed destination in Settings' own `NavigationStack` (or the next split column in regular width)                             |

### Slots as sheets

A slot is `ChildSlot<AnyObject, T>` with `child?.instance`. Build an `Identifiable` wrapper keyed on the instance
identity (`ObjectIdentifier(instance)`), bind `.sheet(item:)` to it, and in the binding's setter (user swiped the sheet
down) call the component's dismiss method. Never set the binding to `nil` from Swift without telling the component.
Put this in one helper (e.g. `View.slotSheet(_:onDismiss:content:)`) and reuse it for every slot.

Sheets: use system detents (`.medium`, `.large`, custom fractions from the mockups), `.presentationDragIndicator`,
`.presentationBackground` for glass where the design shows it. Nested slots (Entry → AccountPicker, NewAccount →
CurrencyPicker, EntryDetails → date) are sheets presented from the presented view.

### Panels (list–detail)

`ChildPanels` has a **mode** the UI must set: `SINGLE` when compact, `DUAL` (or `TRIPLE` for Home) when regular.

```swift
@Environment(\.horizontalSizeClass) private var sizeClass
…
.onChange(of: sizeClass, initial: true) { _, size in
    component.setMode(mode: size == .regular ? .dual : .single)
}
```

- Regular: `NavigationSplitView { main } detail: { details ?? placeholder }` (Home adds the `content`/third column for
  Extra). Selection in the list calls the component (`onOperationClicked`), never a local selection state.
- Compact: `NavigationStack` with path built from `panels.details`/`panels.extra`; popping calls
  `onBackClicked(toIndex:)` (Home: 0 dashboard, 1 details, 2 extra).
- Mode changes keep the open details (the Kotlin tests guarantee it) — don't recreate views on mode change.
- Decide by **size class**, never by device idiom, orientation or screen size: the same code then works on iPhone,
  iPhone Duo (open/folded/closed), iPad split view and Mac windows.

### Tabs (Liquid Glass)

`MainComponent.tabs` is a `ChildStack` with `bringToFront`; its `items` are the tabs created so far and `active` is the
selected one.

- Use the system `TabView` with `Tab` values (iOS 18 API) so the bar is the native Liquid Glass bar on iOS 26+ and the
  standard bar on 18–25, and
  `.tabViewStyle(.sidebarAdaptable)` so iPad and Mac get the sidebar.
- `selection` is derived from `tabs.active`; setting it calls `onHomeTabClicked()` / `onHistoryTabClicked()`.
- Render each tab's content from the matching `tabs.items` child; a tab not created yet shows nothing until selected.
- The money action ("Log money", `onLogMoneyClicked()`) is the centre/primary action of the bar. Build it from native
  pieces — a `Tab` with `role:` that the system renders separately, `.tabViewBottomAccessory`, or a toolbar item —
  following the mockups. **Don't** hand-draw a tab bar over the system one.
- `.tabBarMinimizeBehavior(.onScrollDown)` where the design wants the bar to shrink.

## Availability (deployment iOS 18.2 / macOS 15.2, SDK 27.1)

The app runs on iOS 18.2+ and macOS 15.2+ and is built with the iOS 27.1 SDK (Xcode 27.1). The iPhone Duo APIs are
written directly under `if #available(iOS 27.1, *)`, next to the fallback for earlier systems (`HStack`/`VStack`/
`ZStack` switched by size class). No more commented `DUO-27.1` blocks.

- Gate a fold layout on the fold itself, not only on the version: `ArrangementView(.split)` halves any window, so
  use it while `readsHinge` reports a dividing fold (Entry A12.4, Budget A12.3) and keep the stack otherwise — on the
  folded Duo (466 × 678 pt) a split pushes Expense off the screen.
- On the Duo the tab bar stands vertically at the side and its items are 44 pt wide: a tab label that is a word
  (Home) becomes its icon where `toolbarVerticalEdge` is non-nil. The Duo reads as regular width, so pick iPad-only
  styling by idiom, not by size class.

- System containers (`TabView`, `NavigationStack`, toolbars, sheets, `List`) get Liquid Glass on 26+ automatically and
  the classic look on 18–25. Prefer them: one code path for every version.
- APIs newer than the deployment target go behind `if #available(iOS 26, macOS 26, *)` (glass: `.glassEffect`,
  `GlassEffectContainer`, `.buttonStyle(.glass)`, `.tabViewBottomAccessory`, `.tabBarMinimizeBehavior`) or, once on
  `iOS 27.1` (iPhone Duo: `ArrangementView`, `reservedRegions`, `onHingeChange`, `toolbarVerticalEdge`). The fallback is the native
  equivalent of that version (materials such as `.regularMaterial`, `.borderedProminent`, a plain split/stack),
  never a hand-drawn imitation of glass.
- Wrap each such pair once in a small modifier in `Component/` (e.g. `.talkGlass()`), so screens don't repeat
  `#available` checks.

## Back, swipe-back, keyboard

- Swipe-back and the back button come from `NavigationStack`; they reach Kotlin through the path setter →
  `onBackClicked(toIndex:)`. Long-press on Back (the history menu) works the same way.
- Sheets: swipe-down → the slot's dismiss method; the Entry sheet asks first when `hasChanges` (the component
  shows the discard dialog; use `.interactiveDismissDisabled(model.hasChanges)` and forward the attempt).
- Mac: ⌘W/Esc close sheets through the same dismiss methods; ⌘[ goes back.

## Lifecycle and restoration

- The root lifecycle follows `scenePhase` (`iOSApp.swift`: `onActive/onInactive/onBackground`). Components recompute
  "today" on resume — Swift doesn't need to.
- State restoration: `AppDelegate` saves `appRoot.saveState()` with `StateKeeperUtils` (UIKit secure restoration). The
  Mac app needs the equivalent through its `NSApplicationDelegate` (or the scene's state restoration).
- Create the root once per process (`AppDelegate.appRoot`); never create components in a view `init` that SwiftUI
  may call many times.

## Previews

Every feature has `Preview…Component` classes in Kotlin (`feature/*/src/commonMain/.../Preview*.kt`) with static
models. Use them in `#Preview` blocks, light and dark, at several sizes (`.previewLayout`, traits for compact/regular).
If a preview component is missing a state you need, add a constructor parameter on the Kotlin side.

## Don'ts

- Don't keep a Swift navigation state (`@State var path`) that can drift from the component stack.
- Don't use `UIScreen.main`, device idiom or orientation for layout decisions.
- Don't block the main thread waiting on Kotlin; there is nothing to wait on — `Value`s push changes.
- Don't format money or dates in Kotlin for display; models carry data, Swift formats for the locale.
