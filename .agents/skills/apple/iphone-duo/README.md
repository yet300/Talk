# iPhone Duo skills

Vendored from [mirzaaghazadeh/iphone-duo-skills](https://github.com/mirzaaghazadeh/iphone-duo-skills)
(commit `c5943dc`, 11 October 2026, MIT — see `LICENSE`). Not affiliated with Apple.

Kept: the SwiftUI/UIKit skills Talk needs. Left out: `iphone-duo-camera`, `iphone-duo-games`,
`iphone-duo-flutter`, `iphone-duo-react-native`, `iphone-duo-capacitor` (links to them stay dangling).

Start with `iphone-duo-readiness/SKILL.md`; API names and SDK gates are in `reference/api-index.md`.

**SDK gate:** reserved regions, `ArrangementView`, vertical bars and the hinge APIs need the **iOS 27.1 SDK
(Xcode 27.1)**. Talk builds with it: write these APIs directly under `#available(iOS 27.1, *)` with the size-class
fallback for earlier systems (no commented blocks). Xcode 27.1's iOS 27.1 runtime runs only the iPhone Duo
simulator; snapshots and UI tests run there. Unfolding needs Device Hub's hinge control in Simulator.app — `simctl`
has none.
