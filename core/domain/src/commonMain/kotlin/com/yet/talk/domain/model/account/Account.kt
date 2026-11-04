package com.yet.talk.domain.model.account

import com.yet.talk.domain.model.user.UserId

data class Account(val userId: UserId, val npub: String)
