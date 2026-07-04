package com.hermex.app.data.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val messageId: String?,
    val role: String?,
    val content: String?,
    val timestamp: Double?,
    val reasoning: String?,
    val toolCallsJson: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
