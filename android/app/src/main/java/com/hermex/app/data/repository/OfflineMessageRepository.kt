package com.hermex.app.data.repository

import android.content.Context
import com.hermex.app.data.model.ChatMessage
import com.hermex.app.data.offline.AppDatabase
import com.hermex.app.data.offline.MessageEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class OfflineMessageRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).messageDao()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getMessages(sessionId: String): List<ChatMessage> {
        return dao.getMessages(sessionId).map { it.toChatMessage() }
    }

    suspend fun cacheMessages(sessionId: String, messages: List<ChatMessage>) {
        val entities = messages.map { it.toEntity(sessionId) }
        dao.insertMessages(entities)
        val count = dao.getMessageCount(sessionId)
        if (count > MAX_MESSAGES_PER_SESSION) {
            val excess = count - MAX_MESSAGES_PER_SESSION
            dao.trimMessages(sessionId, excess, MAX_MESSAGES_PER_SESSION)
        }
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
            reasoning = reasoning
        )
    }

    companion object {
        private const val MAX_MESSAGES_PER_SESSION = 50
    }
}
