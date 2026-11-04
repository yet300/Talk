package com.yet.talk.nostr

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventTest {

    // Real kind 1 events from public relays (2026-11): quotes and Arabic text; newlines, imeta and markers.
    private val quotedUnicode = Json.decodeFromString<Event>("""{"content": "{\"id\":\"p1793800122118312\",\"n\":\"هررررر\",\"t\":\"\",\"ts\":1793800122125,\"ty\":\"p\"}", "created_at": 1793800122, "id": "f50bacba21c24630fb4ac0b94bd1a1b1cd5f3a478eb04fc366304ee9a0d48c49", "kind": 1, "pubkey": "5e868749cb940beefa93299fb7a4062427d84451ecae99517acfff5ac99029d1", "sig": "b4d485809e27dff91dda22b8c7bcce9c9ceff9ff2b38a7bace78e90fb980925c6afec5e55a2e2cf7f2a650b546f1f67e4755ac17a7ac0194299136bff1cd150c", "tags": [["t", "tlgblue-dm-45dda58cca595080acebc0b5"], ["d", "presence"]]}""")
    private val multiline = Json.decodeFromString<Event>("""{"content": "Beating the odds like Ike beat on Tina \n\n#M\n\n\nhttps://blossom.primal.net/44157cc15c7aa96edf42c792e5085f5271315dea2507db4d4bbbdf75afd74c6d.gif\nnostr:nevent1qqsvd0c4rjkye53fam8cp0w0u2t4hz45dmkle7grp74qz355n95y7dgjunuh6", "created_at": 1793800173, "id": "d9be1ee0493d71fd4959a5fd103049aeb44d91ec571c73ceac0fb85814312b81", "kind": 1, "pubkey": "8bf7f31116a543f10f82b4f6aa13743b0ef4a7f215767d8931fc8ba917912632", "sig": "43469a25b38f8f8118a3eff54470944daed3cacdddcef816826a8148b91199f96e1fdc439d082bc90e136083c77bbeec26ef27057d6396c9b879f5a0b15b5c8b", "tags": [["e", "c6bf151cac4cd229eecf80bdcfe2975b8ab46eedfcf9030faa01469499684f35", "", "mention"], ["p", "e83b66a8ed2d37c07d1abea6e1b000a15549c69508fa4c5875556d52b0526c2b", "", "mention"], ["p", "cdecc31c6d9406e9a7d6b0067412aa661d9d31c8035c3fd65c06301d1cac3b92", "", "mention"], ["t", "M"], ["imeta", "url https://blossom.primal.net/44157cc15c7aa96edf42c792e5085f5271315dea2507db4d4bbbdf75afd74c6d.gif", "m image/gif", "dim 400.0x300.0"], ["client", "Primal iOS"]]}""")

    @Test
    fun computesTheSameIdAsOtherClients() {
        assertEquals(quotedUnicode.id, quotedUnicode.computeId())
        assertEquals(multiline.id, multiline.computeId())
    }

    @Test
    fun verifiesRealEvents() {
        assertTrue(EventVerifier.verify(quotedUnicode))
        assertTrue(EventVerifier.verify(multiline))
    }

    @Test
    fun rejectsTamperedEvents() {
        assertFalse(EventVerifier.verify(multiline.copy(content = multiline.content + "!")))
        assertFalse(EventVerifier.verify(multiline.copy(tags = emptyList())))
        assertFalse(EventVerifier.verify(multiline.copy(sig = quotedUnicode.sig)))
        assertFalse(EventVerifier.verify(multiline.copy(id = "zz")))
    }

    @Test
    fun escapesStringsLikeJsonStringify() {
        val serialized = EventHash.serialize(
            pubkey = "ab",
            createdAt = 1,
            kind = 1,
            tags = listOf(listOf("t", "x\"y")),
            content = "a\nb\tc\\d\r\u0008\u000C\u0001é/",
        )
        assertEquals(
            """[0,"ab",1,1,[["t","x\"y"]],"a\nb\tc\\d\r\b\f\u0001é/"]""",
            serialized,
        )
    }

    @Test
    fun localSignerProducesVerifiableEvents() = runTest {
        val key = SecretKey.generate()
        val signer = LocalKeySigner(key)
        val event = signer.sign(EventTemplate(kind = 1, content = "hello\nworld", tags = listOf(listOf("t", "talk")), createdAt = 1_700_000_000))

        assertEquals(key.publicKey, event.pubkey)
        assertEquals(key.publicKey, signer.publicKey())
        assertTrue(EventVerifier.verify(event))
    }

    @Test
    fun secretKeyNeverPrintsItself() {
        val key = SecretKey.generate()
        assertFalse(key.toHex() in key.toString())
        assertEquals(key, SecretKey.fromHex(key.toHex()))
    }
}
