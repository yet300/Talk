package com.yet.talk.domain.model.relay

data class Relay(val url: String, val read: Boolean = true, val write: Boolean = true)
