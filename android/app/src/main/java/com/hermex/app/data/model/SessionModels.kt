package com.hermex.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SessionSummary(
    @SerialName("session_id") val sessionId: String? = null,
    val title: String? = null,
    @SerialName("display_title") val displayTitle: String? = null,
    val workspace: String? = null,
    @SerialName("worktree_path") val worktreePath: String? = null,
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null,
    @SerialName("message_count") val messageCount: Int? = null,
    @SerialName("created_at") val createdAt: Double? = null,
    @SerialName("updated_at") val updatedAt: Double? = null,
    @SerialName("last_message_at") val lastMessageAt: Double? = null,
    val pinned: Boolean? = null,
    val archived: Boolean? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("parent_session_id") val parentSessionId: String? = null,
    @SerialName("relationship_type") val relationshipType: String? = null,
    val profile: String? = null,
    @SerialName("input_tokens") val inputTokens: Double? = null,
    @SerialName("output_tokens") val outputTokens: Double? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
    @SerialName("active_stream_id") val activeStreamId: String? = null,
    @SerialName("is_streaming") val isStreaming: Boolean? = null,
    @SerialName("is_cli_session") val isCliSession: Boolean? = null,
    @SerialName("source_tag") val sourceTag: String? = null,
    @SerialName("session_source") val sessionSource: String? = null,
    @SerialName("source_label") val sourceLabel: String? = null,
    @SerialName("match_type") val matchType: String? = null,
    @SerialName("cache_hit_percent") val cacheHitPercent: Double? = null,
    @SerialName("window_usage_percent") val windowUsagePercent: Double? = null,
    @SerialName("read_only") val readOnly: Boolean? = null,
    val attention: String? = null
) {
    val isCronSession: Boolean
        get() = sourceTag == "cron" || sessionSource == "cron"

    val effectiveDisplayTitle: String
        get() = displayTitle?.trim()?.ifEmpty { null }
            ?: title?.trim()?.ifEmpty { null }
            ?: "Untitled Session"

    val displayModel: String
        get() = model?.trim()?.ifEmpty { null } ?: "No model"

    val effectiveTimestamp: Double
        get() = lastMessageAt ?: updatedAt ?: createdAt ?: 0.0

    companion object {
        fun from(detail: SessionDetail): SessionSummary = SessionSummary(
            sessionId = detail.sessionId,
            title = detail.title,
            workspace = detail.workspace,
            model = detail.model,
            modelProvider = detail.modelProvider,
            messageCount = detail.messageCount,
            createdAt = detail.createdAt,
            updatedAt = detail.updatedAt,
            lastMessageAt = detail.lastMessageAt,
            pinned = detail.pinned,
            archived = detail.archived,
            projectId = detail.projectId,
            profile = detail.profile,
            inputTokens = detail.inputTokens,
            outputTokens = detail.outputTokens,
            estimatedCost = detail.estimatedCost,
            activeStreamId = detail.activeStreamId,
            isStreaming = detail.isStreaming,
            isCliSession = detail.isCliSession,
            sourceTag = detail.sourceTag,
            sessionSource = detail.sessionSource,
            sourceLabel = detail.sourceLabel
        )
    }
}

@Serializable
data class SessionDetail(
    @SerialName("session_id") val sessionId: String? = null,
    val title: String? = null,
    val workspace: String? = null,
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null,
    @SerialName("message_count") val messageCount: Int? = null,
    @SerialName("created_at") val createdAt: Double? = null,
    @SerialName("updated_at") val updatedAt: Double? = null,
    @SerialName("last_message_at") val lastMessageAt: Double? = null,
    val pinned: Boolean? = null,
    val archived: Boolean? = null,
    @SerialName("project_id") val projectId: String? = null,
    val profile: String? = null,
    @SerialName("input_tokens") val inputTokens: Double? = null,
    @SerialName("output_tokens") val outputTokens: Double? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
    @SerialName("active_stream_id") val activeStreamId: String? = null,
    @SerialName("is_streaming") val isStreaming: Boolean? = null,
    @SerialName("is_cli_session") val isCliSession: Boolean? = null,
    @SerialName("source_tag") val sourceTag: String? = null,
    @SerialName("session_source") val sessionSource: String? = null,
    @SerialName("source_label") val sourceLabel: String? = null,
    val messages: List<ChatMessage>? = null,
    @SerialName("tool_calls") val toolCalls: List<PersistedToolCall>? = null,
    @SerialName("messages_truncated") val messagesTruncated: Boolean? = null,
    @SerialName("messages_offset") val messagesOffset: Int? = null,
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("threshold_tokens") val thresholdTokens: Int? = null,
    @SerialName("last_prompt_tokens") val lastPromptTokens: Int? = null,
    @SerialName("pending_user_message") val pendingUserMessage: String? = null,
    @SerialName("pending_attachments") val pendingAttachments: JsonElement? = null,
    @SerialName("pending_started_at") val pendingStartedAt: Double? = null,
    @SerialName("compression_anchor_visible_idx") val compressionAnchorVisibleIdx: Int? = null,
    @SerialName("compression_anchor_message_key") val compressionAnchorMessageKey: CompressionAnchorMessageKey? = null,
    @SerialName("compression_anchor_summary") val compressionAnchorSummary: String? = null
)

@Serializable
data class CompressionAnchorMessageKey(
    val role: String? = null,
    val ts: Double? = null,
    val text: String? = null,
    val attachments: Int? = null
)

@Serializable
data class PersistedToolCall(
    val id: String? = null,
    val name: String? = null,
    val preview: String? = null,
    val args: JsonElement? = null,
    val duration: Double? = null,
    @SerialName("is_error") val isError: Boolean? = null,
    @SerialName("anchor_message_id") val anchorMessageId: String? = null
)

@Serializable
data class SessionsResponse(
    val sessions: List<SessionSummary>? = null,
    @SerialName("cli_count") val cliCount: Int? = null,
    @SerialName("server_time") val serverTime: Double? = null,
    @SerialName("server_tz") val serverTz: String? = null
)

@Serializable
data class SessionSearchResponse(
    val sessions: List<SessionSummary>? = null,
    val query: String? = null,
    val count: Int? = null
)

@Serializable
data class SessionResponse(
    val session: SessionDetail? = null
)

@Serializable
data class SessionMutationResponse(
    val ok: Boolean? = null,
    val session: SessionSummary? = null,
    val error: String? = null
)

@Serializable
data class SessionStatusResponse(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("active_stream_id") val activeStreamId: String? = null,
    @SerialName("is_streaming") val isStreaming: Boolean? = null,
    @SerialName("pending_user_message") val pendingUserMessage: String? = null,
    val error: String? = null
)

@Serializable
data class ProjectSummary(
    @SerialName("project_id") val projectId: String? = null,
    val name: String? = null,
    val color: String? = null,
    @SerialName("created_at") val createdAt: Double? = null
)

@Serializable
data class ProjectsResponse(
    val projects: List<ProjectSummary>? = null
)

@Serializable
data class ProjectMutationResponse(
    val ok: Boolean? = null,
    val project: ProjectSummary? = null,
    val error: String? = null
)

@Serializable
data class Workspace(
    val path: String? = null,
    val name: String? = null
)

@Serializable
data class WorkspacesResponse(
    val workspaces: List<Workspace>? = null
) {
    fun last(): String? = workspaces?.lastOrNull()?.path
}

@Serializable
data class WorkspaceSuggestionsResponse(
    val suggestions: List<String>? = null
)
