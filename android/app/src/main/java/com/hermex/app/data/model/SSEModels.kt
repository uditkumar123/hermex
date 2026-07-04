package com.hermex.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

enum class ConnectionState {
    Connected,
    Reconnecting,
    Disconnected
}

sealed class SSEStreamEvent {
    data class Event(val event: SSEEvent) : SSEStreamEvent()
    data class StateChange(val state: ConnectionState) : SSEStreamEvent()
}

sealed class SSEEvent {
    data class Token(val text: String) : SSEEvent()
    data class InterimAssistant(val text: String?, val alreadyStreamed: Boolean?) : SSEEvent()
    data class Reasoning(val text: String) : SSEEvent()
    data class ToolStarted(val event: ToolStreamEvent) : SSEEvent()
    data class ToolCompleted(val event: ToolStreamEvent) : SSEEvent()
    data class Title(val sessionId: String?, val title: String?) : SSEEvent()
    data class Done(val usage: ContextWindowSnapshot?, val session: SessionDetail?) : SSEEvent()
    data class ApprovalPending(val response: ApprovalPendingResponse) : SSEEvent()
    data class ClarificationPending(val response: ClarificationPendingResponse) : SSEEvent()
    data class PendingSteerLeftover(val text: String) : SSEEvent()
    data object StreamEnd : SSEEvent()
    data object Cancelled : SSEEvent()
    data class Error(val message: String) : SSEEvent()
    data class TransportError(val message: String) : SSEEvent()
    data object Ignored : SSEEvent()
}

@Serializable
data class ToolStreamEvent(
    @SerialName("event_type") val eventType: String? = null,
    val name: String? = null,
    val preview: String? = null,
    val args: Map<String, JsonElement>? = null,
    val duration: Double? = null,
    @SerialName("is_error") val isError: Boolean? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_use_id") val toolUseId: String? = null,
    @SerialName("call_id") val callId: String? = null,
    val id: String? = null,
    val tid: String? = null
) {
    val stableId: String?
        get() = listOfNotNull(toolCallId, toolUseId, callId, id, tid).firstOrNull { it.isNotBlank() }

    val displayName: String
        get() = name?.replaceFirstChar { it.uppercase() } ?: "Tool"
}

@Serializable
data class ApprovalPendingResponse(
    val pending: PendingApproval? = null,
    @SerialName("pending_count") val pendingCount: Int? = null
)

@Serializable
data class PendingApproval(
    val id: String? = null,
    val description: String? = null,
    val tool: String? = null,
    val args: JsonElement? = null,
    val preview: String? = null,
    @SerialName("display_pattern_keys") val displayPatternKeys: List<String>? = null
) {
    val displayDescription: String
        get() = description?.trim()?.ifEmpty { null } ?: tool ?: "Approval needed"
}

@Serializable
data class ClarificationPendingResponse(
    val pending: PendingClarification? = null,
    @SerialName("pending_count") val pendingCount: Int? = null
)

@Serializable
data class PendingClarification(
    val id: String? = null,
    val question: String? = null,
    val choices: List<String>? = null
) {
    val displayQuestion: String
        get() = question?.trim()?.ifEmpty { null } ?: "Clarification needed"

    val displayChoices: List<String>
        get() = choices?.filter { it.isNotBlank() } ?: emptyList()
}

@Serializable
data class ApprovalRespondResponse(
    val ok: Boolean? = null,
    val choice: String? = null
)

@Serializable
data class ClarificationRespondResponse(
    val ok: Boolean? = null,
    val response: String? = null
)
