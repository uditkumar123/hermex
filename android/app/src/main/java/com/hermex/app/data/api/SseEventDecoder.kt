package com.hermex.app.data.api

import com.hermex.app.data.model.*
import kotlinx.serialization.json.Json
import timber.log.Timber

object SseEventDecoder {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun decode(eventType: String, data: String): SSEEvent {
        return try {
            when (eventType) {
                "token" -> {
                    val payload = decodePayload<TokenPayload>(data)
                    SSEEvent.Token(payload?.text ?: "")
                }

                "interim_assistant" -> {
                    val payload = decodePayload<InterimAssistantPayload>(data)
                    SSEEvent.InterimAssistant(
                        text = payload?.text,
                        alreadyStreamed = payload?.alreadyStreamed
                    )
                }

                "reasoning" -> {
                    val payload = decodePayload<ReasoningPayload>(data)
                    SSEEvent.Reasoning(payload?.text ?: "")
                }

                "tool" -> {
                    val payload = decodePayload<ToolStreamEvent>(data)
                    if (payload != null) SSEEvent.ToolStarted(payload) else SSEEvent.Ignored
                }

                "tool_complete" -> {
                    val payload = decodePayload<ToolStreamEvent>(data)
                    if (payload != null) SSEEvent.ToolCompleted(payload) else SSEEvent.Ignored
                }

                "title" -> {
                    val payload = decodePayload<TitlePayload>(data)
                    SSEEvent.Title(
                        sessionId = payload?.sessionId,
                        title = payload?.title
                    )
                }

                "done" -> {
                    val payload = decodePayload<DonePayload>(data)
                    if (payload != null) {
                        SSEEvent.Done(
                            usage = payload.usage,
                            session = payload.session
                        )
                    } else {
                        SSEEvent.TransportError("Malformed completion event")
                    }
                }

                "initial", "approval" -> {
                    val payload = decodePayload<ApprovalPendingResponse>(data)
                    if (payload != null) SSEEvent.ApprovalPending(payload) else SSEEvent.Ignored
                }

                "clarify" -> {
                    val payload = decodePayload<ClarificationPendingResponse>(data)
                    if (payload != null) SSEEvent.ClarificationPending(payload) else SSEEvent.Ignored
                }

                "pending_steer_leftover" -> {
                    val payload = decodePayload<PendingSteerLeftoverPayload>(data)
                    SSEEvent.PendingSteerLeftover(payload?.text ?: "")
                }

                "stream_end" -> SSEEvent.StreamEnd
                "cancel" -> SSEEvent.Cancelled

                "error" -> {
                    val payload = decodePayload<ErrorPayload>(data)
                    SSEEvent.Error(payload?.error ?: payload?.message ?: "Stream error")
                }

                "apperror" -> {
                    val payload = decodePayload<ErrorPayload>(data)
                    SSEEvent.Error(payload?.error ?: payload?.message ?: "App error")
                }

                "metering" -> {
                    val payload = decodePayload<MeteringPayload>(data)
                    SSEEvent.Ignored
                }

                "compressed" -> {
                    val payload = decodePayload<CompressedPayload>(data)
                    SSEEvent.Ignored
                }

                "compressing" -> {
                    val payload = decodePayload<CompressingPayload>(data)
                    SSEEvent.Ignored
                }

                "warning" -> {
                    val payload = decodePayload<WarningPayload>(data)
                    SSEEvent.Ignored
                }

                "context_status" -> {
                    val payload = decodePayload<ContextStatusPayload>(data)
                    SSEEvent.Ignored
                }

                "todo_state" -> {
                    val payload = decodePayload<TodoStatePayload>(data)
                    SSEEvent.Ignored
                }

                "goal" -> {
                    val payload = decodePayload<GoalPayload>(data)
                    SSEEvent.Ignored
                }

                "goal_continue" -> {
                    val payload = decodePayload<GoalContinuePayload>(data)
                    SSEEvent.Ignored
                }

                else -> {
                    Timber.d("Ignoring unknown SSE event type: $eventType")
                    SSEEvent.Ignored
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decode SSE event: $eventType")
            SSEEvent.Ignored
        }
    }

    private inline fun <reified T> decodePayload(data: String): T? {
        return try {
            json.decodeFromString<T>(data)
        } catch (e: Exception) {
            null
        }
    }
}

@kotlinx.serialization.Serializable
private data class TokenPayload(val text: String? = null)

@kotlinx.serialization.Serializable
private data class InterimAssistantPayload(
    val text: String? = null,
    @kotlinx.serialization.SerialName("already_streamed") val alreadyStreamed: Boolean? = null
)

@kotlinx.serialization.Serializable
private data class ReasoningPayload(val text: String? = null)

@kotlinx.serialization.Serializable
private data class TitlePayload(
    @kotlinx.serialization.SerialName("session_id") val sessionId: String? = null,
    val title: String? = null
)

@kotlinx.serialization.Serializable
private data class DonePayload(
    val usage: ContextWindowSnapshot? = null,
    val session: SessionDetail? = null
)

@kotlinx.serialization.Serializable
private data class ErrorPayload(
    val error: String? = null,
    val message: String? = null
)

@kotlinx.serialization.Serializable
private data class PendingSteerLeftoverPayload(val text: String? = null)

@kotlinx.serialization.Serializable
private data class MeteringPayload(
    val tokens: Double? = null,
    val cost: Double? = null,
    val provider: String? = null
)

@kotlinx.serialization.Serializable
private data class CompressedPayload(
    @kotlinx.serialization.SerialName("session_id") val sessionId: String? = null,
    val summary: String? = null
)

@kotlinx.serialization.Serializable
private data class CompressingPayload(
    @kotlinx.serialization.SerialName("session_id") val sessionId: String? = null,
    val status: String? = null
)

@kotlinx.serialization.Serializable
private data class WarningPayload(
    val message: String? = null,
    val code: String? = null
)

@kotlinx.serialization.Serializable
private data class ContextStatusPayload(
    @kotlinx.serialization.SerialName("context_length") val contextLength: Int? = null,
    @kotlinx.serialization.SerialName("threshold_tokens") val thresholdTokens: Int? = null,
    @kotlinx.serialization.SerialName("last_prompt_tokens") val lastPromptTokens: Int? = null
)

@kotlinx.serialization.Serializable
private data class TodoStatePayload(
    val todos: List<String>? = null,
    val completed: List<String>? = null
)

@kotlinx.serialization.Serializable
private data class GoalPayload(
    val goal: String? = null,
    @kotlinx.serialization.SerialName("session_id") val sessionId: String? = null
)

@kotlinx.serialization.Serializable
private data class GoalContinuePayload(
    val goal: String? = null,
    val status: String? = null
)
