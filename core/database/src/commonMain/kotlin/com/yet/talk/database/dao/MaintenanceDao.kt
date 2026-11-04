package com.yet.talk.database.dao

interface MaintenanceDao {
    /** Sign-out: everything but public profiles. */
    suspend fun clearUserData()
}
