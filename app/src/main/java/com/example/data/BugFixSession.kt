package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bug_fix_sessions")
data class BugFixSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val buggyCode: String,
    val errorMessage: String,
    val fixedCode: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis()
)
