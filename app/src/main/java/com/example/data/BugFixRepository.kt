package com.example.data

import kotlinx.coroutines.flow.Flow

class BugFixRepository(private val bugFixDao: BugFixDao) {
    val allSessions: Flow<List<BugFixSession>> = bugFixDao.getAllSessions()

    fun getSessionsByLanguage(language: String): Flow<List<BugFixSession>> {
        return bugFixDao.getSessionsByLanguage(language)
    }

    fun searchSessions(query: String): Flow<List<BugFixSession>> {
        return bugFixDao.searchSessions(query)
    }

    suspend fun insert(session: BugFixSession): Long {
        return bugFixDao.insertSession(session)
    }

    suspend fun deleteById(id: Int) {
        bugFixDao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        bugFixDao.clearAll()
    }
}
