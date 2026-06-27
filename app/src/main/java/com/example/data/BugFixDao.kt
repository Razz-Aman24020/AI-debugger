package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BugFixDao {
    @Query("SELECT * FROM bug_fix_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<BugFixSession>>

    @Query("SELECT * FROM bug_fix_sessions WHERE language = :language ORDER BY timestamp DESC")
    fun getSessionsByLanguage(language: String): Flow<List<BugFixSession>>

    @Query("SELECT * FROM bug_fix_sessions WHERE title LIKE '%' || :query || '%' OR buggyCode LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSessions(query: String): Flow<List<BugFixSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BugFixSession): Long

    @Query("DELETE FROM bug_fix_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("DELETE FROM bug_fix_sessions")
    suspend fun clearAll()
}
