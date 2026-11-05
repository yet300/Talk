package com.yet.talk.database.dao

interface MaintenanceDao {
    /** Sign-out: everything but public facts (profiles, NIP-09 tombstones). */
    suspend fun clearUserData()
}
