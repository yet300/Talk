@file:JsModule("@noble/hashes/scrypt.js")
@file:JsNonModule

package com.yet.talk.nostr.crypto

import org.khronos.webgl.Uint8Array
import kotlin.js.Promise

/** Yields to the event loop while it works, so the page stays responsive. */
internal external fun scryptAsync(password: Uint8Array, salt: Uint8Array, options: dynamic): Promise<Uint8Array>
