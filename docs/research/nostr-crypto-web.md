# Research: a Nostr library and crypto for Android, iOS and web

Date: 2026-11-04. Requirement: a single Kotlin domain and data layer for Android, iOS and
**Kotlin/JS (browser)**, with native UI on each platform.

## What a client needs

- NIP-01: the event model, the canonical JSON serialisation, `id = sha256(serialised)`, and the
  relay protocol (`REQ`/`EVENT`/`CLOSE`/`EOSE`/`OK`) over WebSocket.
- BIP-340 Schnorr signatures on secp256k1: sign, verify, and derive the x-only public key.
- bech32 for `npub`/`nsec`/`nevent`. It is about 100 lines of pure Kotlin.
- No encryption for the MVP: KarpathyTalk has no DMs. NIP-44 is only needed for NIP-46 remote
  signers or private bookmarks, so it is deferred (YAGNI).

## Nostr libraries

| Library | Targets | JS | Verdict |
|---|---|---|---|
| [Quartz](https://github.com/vitorpamplona/quartz) (Amethyst) | JVM, Android, iOS, macOS, Linux | **no** | The most complete, but it has no web target |
| [rust-nostr `nostr-sdk-kmp`](https://klibs.io/package/org.rust-nostr/nostr-sdk-kmp) 0.44.2 | Android, iOS, desktop | **no** (the WASM build is a separate JS API) | Two different APIs for web and mobile |
| [knostr](https://github.com/uakihir0/knostr) | JVM, Apple, JS, mingw | yes, but on JS only through NIP-07 | Only `0.0.1-SNAPSHOT` in a private repository, 0 stars |
| [nostr-kmp](https://github.com/NicolaLS/nostr-kmp) | JVM, Android, iOS | no | Archived |

**Conclusion:** no mature KMP Nostr library supports JS. NIP-01 is small, so we write a thin
`core:nostr` module ourselves and keep the crypto behind an interface.

## Crypto (Schnorr / secp256k1)

| Option | Android | iOS | JS | Notes |
|---|---|---|---|---|
| [ACINQ secp256k1-kmp](https://github.com/ACINQ/secp256k1-kmp) 0.24.0 | ✅ JNI | ✅ cinterop | ❌ | Wraps Bitcoin Core's libsecp256k1, the reference implementation; Schnorr is supported; used in production by Phoenix and Quartz |
| [@noble/curves](https://github.com/paulmillr/noble-curves) 2.4.0 (npm) | — | — | ✅ | Audited, pure JS, used by nostr-tools; `schnorr` follows BIP-340 |
| [libschnorr256k1-kmp](https://github.com/vitorpamplona/libschnorr256k1-kmp) 1.0.5 | ✅ | ✅ | ✅ WASM | One API everywhere, but young (1 star) and its WASM initialisation is asynchronous |
| [KotlinCrypto hash](https://github.com/KotlinCrypto/hash) `sha2` 0.8.0 | ✅ | ✅ | ✅ | Pure Kotlin SHA-256 for event ids on every target |

## Recommendation

```
core:nostr (commonMain)
 ├─ Event, Filter, RelayMessage      kotlinx.serialization, canonical JSON
 ├─ EventId = sha256(...)            org.kotlincrypto.hash:sha2
 ├─ RelayPool                        Ktor WebSockets (OkHttp / Darwin / Js engines)
 ├─ Bech32 (npub/nsec/note/nevent)   pure Kotlin
 └─ interface Signer { pubKey; suspend fun sign(unsigned): Event }
    interface SchnorrVerifier { fun verify(id, sig, pubKey): Boolean }

actual implementations
 ├─ androidMain / iosMain  → fr.acinq.secp256k1:secp256k1-kmp (+ jni-android)
 └─ jsMain                 → npm @noble/curves via `external` declarations

Signer implementations (data layer)
 ├─ LocalKeySigner   nsec in KSafe                  Android, iOS
 ├─ AmberSigner      NIP-55                         Android (later)
 └─ Nip07Signer      window.nostr                   Web
```

Why:

1. The best-reviewed implementation on each platform: libsecp256k1 on mobile, noble on web.
2. `Signer` is `suspend`, so the asynchronous paths (NIP-07, Amber, WASM) fit without changes
   to the domain layer.
3. Swapping the backend later, for example to libschnorr256k1-kmp once it matures, touches
   only the `actual` files.

Version catalog entries to add together with `core:nostr`:

```toml
secp256k1 = "0.24.0"
kotlincrypto-hash = "0.8.0"
ktor-client-js = …            # already added
# npm (jsMain): @noble/curves 2.4.0
```

## Markdown rendering (native UI)

[Enriched Markdown](https://github.com/software-mansion/enriched-markdown) (Software Mansion):

- **Android:** `com.swmansion.enriched.markdown:compose:0.3.0` is an Android-only AAR, so it
  belongs in Android UI code, not in commonMain.
- **iOS:** a Swift package for SwiftUI (`enriched-markdown-ios`).
- **Web:** available only through React Native for Web. Our web app is plain React + Vite, so
  it needs either react-native-web or a regular React renderer (for example `react-markdown`).
- On native platforms GFM is marked "available soon", while CommonMark is ready. Check GFM
  tables and syntax highlighting before relying on them: KarpathyTalk posts use GFM.
