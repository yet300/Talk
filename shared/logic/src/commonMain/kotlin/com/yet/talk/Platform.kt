package com.yet.talk

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform