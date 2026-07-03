package com.hermex.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessage(
    val role: String? = null,
    val content: String? = null,
    val timestamp: Double? = null,
    @SerialName("message_id") val messageId: String? = null,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_use_id") val toolUseId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<JsonElement>? = null,
    @SerialName("content_parts") val contentParts: List<JsonElement>? = null,
    val reasoning: String? = null,
    val attachments: List<MessageAttachment>? = null
) {
    val isUser: Boolean get() = role == "user"
    val isAssistant: Boolean get() = role == "assistant"
    val isSystem: Boolean get() = role == "system"
    val isToolResult: Boolean get() = role == "toolResult"

    val displayContent: String
        get() = content?.trim()?.ifEmpty { null } ?: ""

    val hasToolCalls: Boolean
        get() = !toolCalls.isNullOrEmpty()

    val hasReasoning: Boolean
        get() = !reasoning.isNullOrBlank()

    val hasAttachments: Boolean
        get() = !attachments.isNullOrEmpty()

    val displayTimestamp: Double
        get() = timestamp ?: 0.0
}

@Serializable
data class MessageAttachment(
    val name: String? = null,
    val path: String? = null,
    val mime: String? = null,
    val size: Long? = null,
    @SerialName("is_image") val isImage: Boolean? = null
) {
    val isImageAttachment: Boolean
        get() = isImage == true || (mime?.startsWith("image/") == true)

    val displayName: String
        get() = name ?: path?.substringAfterLast('/') ?: "Attachment"
}

@Serializable
data class ChatStartResponse(
    @SerialName("stream_id") val streamId: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    val error: String? = null
)

@Serializable
data class ChatCancelResponse(
    val ok: Boolean? = null,
    val cancelled: Boolean? = null,
    @SerialName("stream_id") val streamId: String? = null,
    val error: String? = null
)

@Serializable
data class ChatStreamStatusResponse(
    val active: Boolean? = null,
    @SerialName("stream_id") val streamId: String? = null,
    @SerialName("replay_available") val replayAvailable: Boolean? = null
)

@Serializable
data class ChatSteerResponse(
    val accepted: Boolean? = null,
    val fallback: Boolean? = null,
    @SerialName("stream_id") val streamId: String? = null,
    val error: String? = null
)

@Serializable
data class ContextWindowSnapshot(
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("threshold_tokens") val thresholdTokens: Int? = null,
    @SerialName("last_prompt_tokens") val lastPromptTokens: Int? = null,
    @SerialName("input_tokens") val inputTokens: Double? = null,
    @SerialName("output_tokens") val outputTokens: Double? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null
)

@Serializable
data class ModelsResponse(
    val groups: List<JsonElement>? = null,
    val models: List<JsonElement>? = null,
    @SerialName("default_model") val defaultModel: String? = null,
    @SerialName("active_provider") val activeProvider: String? = null
)

@Serializable
data class ModelsLiveResponse(
    val provider: String? = null,
    val models: List<JsonElement>? = null,
    val count: Int? = null
)

@Serializable
data class DefaultModelResponse(
    val ok: Boolean? = null,
    val model: String? = null
)

@Serializable
data class ProfilesResponse(
    val profiles: List<ProfileSummary>? = null,
    val active: String? = null
)

@Serializable
data class ProfileSummary(
    val name: String? = null,
    val display_name: String? = null,
    val model: String? = null,
    val provider: String? = null,
    val personality: String? = null
) {
    val displayName: String
        get() = display_name ?: name?.replaceFirstChar { it.uppercase() } ?: "Default"

    val normalizedName: String?
        get() = name?.trim()?.ifEmpty { null }
}

@Serializable
data class ProfileSwitchResponse(
    val profiles: List<ProfileSummary>? = null,
    val active: String? = null,
    @SerialName("default_model") val defaultModel: String? = null,
    @SerialName("default_workspace") val defaultWorkspace: String? = null,
    val error: String? = null
)

@Serializable
data class SettingsResponse(
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("webui_version") val webuiVersion: String? = null,
    val version: String? = null,
    val theme: String? = null
)
