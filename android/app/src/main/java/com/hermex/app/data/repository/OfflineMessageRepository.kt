package com.hermex.app.data.repository

import android.content.Context
import com.hermex.app.data.model.ChatMessage
import com.hermex.app.data.offline.AppDatabase
import com.hermex.app.data.offline.MessageEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class OfflineMessageRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).messageDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getMessages(sessionId: String): List<ChatMessage> {
        return dao.getMessages(sessionId).map { it.toChatMessage() }
    }

    suspend fun cacheMessages(sessionId: String, messages: List<ChatMessage>) {
        val entities = messages.takeLast(MAX_MESSAGES_PER_SESSION).map { it.toEntity(sessionId) }
        dao.deleteMessages(sessionId)
        dao.insertMessages(entities)
    }

    suspend fun deleteMessages(sessionId: String) {
        dao.deleteMessages(sessionId)
    }

    private fun ChatMessage.toEntity(sessionId: String): MessageEntity {
        return MessageEntity(
            sessionId = sessionId,
            messageId = messageId,
            role = role,
            content = content,
            timestamp = timestamp,
            reasoning = reasoning,
            toolCallsJson = if (toolCalls.isNullOrEmpty()) null else json.encodeToString(toolCalls)
        )
    }

    private fun MessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            timestamp = timestamp,
            messageId = messageId,
            reasoning = reasoning,
            toolCalls = toolCallsJson?.let { encoded ->
                runCatching { json.decodeFromString<List<JsonElement>>(encoded) }.getOrNull()
            }
        )
    }

    companion object {
        private const val MAX_MESSAGES_PER_SESSION = 50
    }
}
