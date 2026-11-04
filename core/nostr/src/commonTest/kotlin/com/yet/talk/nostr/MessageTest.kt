package com.yet.talk.nostr

import com.yet.talk.nostr.message.ClientMessage
import com.yet.talk.nostr.message.Filter
import com.yet.talk.nostr.message.RelayMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MessageTest {

    @Test
    fun serializesRequestsWithTagFilters() {
        val request = ClientMessage.Req(
            subscriptionId = "s1",
            filters = listOf(Filter(kinds = listOf(1), tags = mapOf('L' to listOf("com.yet.talk")), until = 10, limit = 20)),
        )
        assertEquals("""["REQ","s1",{"kinds":[1],"#L":["com.yet.talk"],"until":10,"limit":20}]""", request.toJson())
        assertEquals("""["CLOSE","s1"]""", ClientMessage.Close("s1").toJson())
    }

    @Test
    fun parsesRelayMessages() {
        val event = """{"id":"a","pubkey":"b","created_at":1,"kind":1,"tags":[],"content":"x","sig":"c","extra":true}"""
        assertEquals("x", assertIs<RelayMessage.EventMessage>(RelayMessage.parse("""["EVENT","s",$event]""")).event.content)
        assertEquals(RelayMessage.EndOfStoredEvents("s"), RelayMessage.parse("""["EOSE","s"]"""))
        assertEquals(RelayMessage.Ok("a", false, "blocked: spam"), RelayMessage.parse("""["OK","a",false,"blocked: spam"]"""))
        assertEquals(RelayMessage.Closed("s", "error: no"), RelayMessage.parse("""["CLOSED","s","error: no"]"""))
        assertEquals(RelayMessage.CountResult("s", 42), RelayMessage.parse("""["COUNT","s",{"count":42}]"""))
        assertEquals(RelayMessage.Notice("hi"), RelayMessage.parse("""["NOTICE","hi"]"""))
        assertEquals(RelayMessage.Auth("challenge"), RelayMessage.parse("""["AUTH","challenge"]"""))
    }

    @Test
    fun ignoresMalformedFrames() {
        assertNull(RelayMessage.parse("not json"))
        assertNull(RelayMessage.parse("""["EVENT","s",{"id":1}]"""))
        assertNull(RelayMessage.parse("""["WHAT","s"]"""))
        assertNull(RelayMessage.parse("{}"))
    }
}
