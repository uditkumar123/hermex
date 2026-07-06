package com.hermex.app.data.api

import com.hermex.app.data.model.*
import kotlinx.serialization.SerialName
import retrofit2.http.*

interface HermesApi {

    // ── Auth & Health ──────────────────────────────────────────────

    @GET("/health")
    suspend fun health(): HealthResponse

    @GET("/api/auth/status")
    suspend fun authStatus(): AuthStatusResponse

    @POST("/api/auth/login")
    @Headers("Content-Type: application/json")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("/api/auth/logout")
    @Headers("Content-Type: application/json")
    suspend fun logout(@Body body: Map<String, String> = emptyMap()): LoginResponse

    // ── Sessions ───────────────────────────────────────────────────

    @GET("/api/sessions")
    suspend fun sessions(): SessionsResponse

    @GET("/api/sessions/search")
    suspend fun searchSessions(
        @Query("q") query: String,
        @Query("content") content: Int = 1,
        @Query("depth") depth: Int = 5
    ): SessionSearchResponse

    @GET("/api/session")
    suspend fun session(
        @Query("session_id") sessionId: String,
        @Query("messages") includeMessages: Int = 1,
        @Query("msg_limit") messageLimit: Int = 50,
        @Query("msg_before") messageBefore: Int? = null,
        @Query("expand_renderable") expandRenderable: Int? = null
    ): SessionResponse

    @GET("/api/session/status")
    suspend fun sessionStatus(
        @Query("session_id") sessionId: String
    ): SessionStatusResponse

    @POST("/api/session/new")
    @Headers("Content-Type: application/json")
    suspend fun createSession(@Body body: CreateSessionRequest): SessionResponse

    @POST("/api/session/rename")
    @Headers("Content-Type: application/json")
    suspend fun renameSession(@Body body: RenameSessionRequest): SessionMutationResponse

    @POST("/api/session/delete")
    @Headers("Content-Type: application/json")
    suspend fun deleteSession(@Body body: SessionIdRequest): SessionMutationResponse

    @POST("/api/session/pin")
    @Headers("Content-Type: application/json")
    suspend fun pinSession(@Body body: PinSessionRequest): SessionMutationResponse

    @POST("/api/session/archive")
    @Headers("Content-Type: application/json")
    suspend fun archiveSession(@Body body: ArchiveSessionRequest): SessionMutationResponse

    @POST("/api/session/branch")
    @Headers("Content-Type: application/json")
    suspend fun branchSession(@Body body: BranchSessionRequest): SessionMutationResponse

    @POST("/api/session/compress")
    @Headers("Content-Type: application/json")
    suspend fun compressSession(@Body body: CompressSessionRequest): SessionMutationResponse

    @POST("/api/session/undo")
    @Headers("Content-Type: application/json")
    suspend fun undoSession(@Body body: SessionIdRequest): SessionMutationResponse

    @POST("/api/session/retry")
    @Headers("Content-Type: application/json")
    suspend fun retrySession(@Body body: SessionIdRequest): SessionMutationResponse

    @POST("/api/session/update")
    @Headers("Content-Type: application/json")
    suspend fun updateSession(@Body body: UpdateSessionRequest): SessionResponse

    @POST("/api/session/move")
    @Headers("Content-Type: application/json")
    suspend fun moveSession(@Body body: MoveSessionRequest): SessionMutationResponse

    @GET("/api/projects")
    suspend fun projects(@Query("all_profiles") allProfiles: Int? = null): ProjectsResponse

    // ── Chat ───────────────────────────────────────────────────────

    @POST("/api/chat/start")
    @Headers("Content-Type: application/json")
    suspend fun startChat(@Body body: ChatStartRequest): ChatStartResponse

    @GET("/api/chat/cancel")
    suspend fun cancelChat(@Query("stream_id") streamId: String): ChatCancelResponse

    @GET("/api/chat/stream/status")
    suspend fun chatStreamStatus(@Query("stream_id") streamId: String): ChatStreamStatusResponse

    @POST("/api/chat/steer")
    @Headers("Content-Type: application/json")
    suspend fun steerChat(@Body body: ChatSteerRequest): ChatSteerResponse

    @POST("/api/chat/approval/respond")
    @Headers("Content-Type: application/json")
    suspend fun respondToApproval(@Body body: ApprovalRespondRequest): ApprovalRespondResponse

    @POST("/api/chat/clarify/respond")
    @Headers("Content-Type: application/json")
    suspend fun respondToClarification(@Body body: ClarificationRespondRequest): ClarificationRespondResponse

    // ── Models ─────────────────────────────────────────────────────

    @GET("/api/models")
    suspend fun models(): ModelsResponse

    @GET("/api/models/live")
    suspend fun modelsLive(): ModelsLiveResponse

    @POST("/api/default-model")
    @Headers("Content-Type: application/json")
    suspend fun saveDefaultModel(@Body body: DefaultModelRequest): DefaultModelResponse

    // ── Workspaces ─────────────────────────────────────────────────

    @GET("/api/workspaces")
    suspend fun workspaces(): WorkspacesResponse

    @GET("/api/workspaces/suggest")
    suspend fun workspaceSuggestions(@Query("prefix") prefix: String): WorkspaceSuggestionsResponse

    @GET("/api/list")
    suspend fun listFiles(
        @Query("session_id") sessionId: String,
        @Query("path") path: String? = null
    ): FileListResponse

    @GET("/api/file")
    suspend fun getFile(
        @Query("session_id") sessionId: String,
        @Query("path") path: String
    ): FileContentResponse

    @Streaming
    @GET("/api/file/raw")
    suspend fun getRawFileStream(
        @Query("session_id") sessionId: String,
        @Query("path") path: String
    ): okhttp3.ResponseBody

    // ── Profiles ───────────────────────────────────────────────────

    @GET("/api/profiles")
    suspend fun profiles(): ProfilesResponse

    @POST("/api/profile/switch")
    @Headers("Content-Type: application/json")
    suspend fun switchProfile(@Body body: SwitchProfileRequest): ProfileSwitchResponse

    // ── Settings ───────────────────────────────────────────────────

    @GET("/api/settings")
    suspend fun settings(): SettingsResponse

    // ── Skills ──────────────────────────────────────────────────────

    @GET("/api/skills")
    suspend fun skills(): SkillsResponse

    @GET("/api/skills/content")
    suspend fun skillContent(@Query("name") name: String): SkillContentResponse

    // ── Memory ──────────────────────────────────────────────────────

    @GET("/api/memory")
    suspend fun memory(): MemoryResponse

    // ── Session Export/Import ──────────────────────────────────────

    @GET("/api/session/export")
    suspend fun exportSession(
        @Query("session_id") sessionId: String,
        @Query("format") format: String? = null
    ): SessionExportResponse

    @POST("/api/session/import")
    @Headers("Content-Type: application/json")
    suspend fun importSession(@Body body: SessionImportRequest): SessionResponse

    // ── YOLO Mode ─────────────────────────────────────────────────

    @POST("/api/session/yolo")
    @Headers("Content-Type: application/json")
    suspend fun yoloSession(@Body body: SessionIdRequest): SessionMutationResponse

    // ── Goal & BTW ────────────────────────────────────────────────

    @POST("/api/goal")
    @Headers("Content-Type: application/json")
    suspend fun setGoal(@Body body: GoalRequest): GoalResponse

    @POST("/api/btw")
    @Headers("Content-Type: application/json")
    suspend fun btw(@Body body: BtwRequest): BtwResponse

    // ── Background Tasks ──────────────────────────────────────────

    @GET("/api/background/status")
    suspend fun backgroundStatus(
        @Query("session_id") sessionId: String
    ): BackgroundStatusResponse

    @POST("/api/background/start")
    @Headers("Content-Type: application/json")
    suspend fun startBackground(@Body body: BackgroundStartRequest): BackgroundStartResponse

    @POST("/api/background/stop")
    @Headers("Content-Type: application/json")
    suspend fun stopBackground(@Body body: SessionIdRequest): BackgroundStopResponse

    // ── TTS ───────────────────────────────────────────────────────

    @POST("/api/tts")
    @Headers("Content-Type: application/json")
    suspend fun tts(@Body body: TtsRequest): TtsResponse

    // ── Profile Active ────────────────────────────────────────────

    @GET("/api/profile/active")
    suspend fun activeProfile(): ProfileActiveResponse

    @POST("/api/profile/active")
    @Headers("Content-Type: application/json")
    suspend fun setActiveProfile(@Body body: ProfileActiveRequest): ProfileActiveResponse

    // ── Metrics ───────────────────────────────────────────────────

    @POST("/api/metrics/track")
    @Headers("Content-Type: application/json")
    suspend fun trackMetrics(@Body body: MetricsTrackRequest): MetricsTrackResponse

    // ── Last Commit ───────────────────────────────────────────────

    @GET("/api/last-commit")
    suspend fun lastCommit(
        @Query("session_id") sessionId: String
    ): LastCommitResponse

    // ── Agent Restart ─────────────────────────────────────────────

    @POST("/api/agent/restart")
    @Headers("Content-Type: application/json")
    suspend fun restartAgent(@Body body: AgentRestartRequest): AgentRestartResponse
}

// ── Request bodies ──────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class LoginRequest(val password: String)

@kotlinx.serialization.Serializable
data class CreateSessionRequest(
    val workspace: String? = null,
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null,
    val profile: String? = null
)

@kotlinx.serialization.Serializable
data class RenameSessionRequest(
    @SerialName("session_id") val sessionId: String,
    val title: String
)

@kotlinx.serialization.Serializable
data class SessionIdRequest(
    @SerialName("session_id") val sessionId: String
)

@kotlinx.serialization.Serializable
data class PinSessionRequest(
    @SerialName("session_id") val sessionId: String,
    val pinned: Boolean
)

@kotlinx.serialization.Serializable
data class ArchiveSessionRequest(
    @SerialName("session_id") val sessionId: String,
    val archived: Boolean
)

@kotlinx.serialization.Serializable
data class BranchSessionRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("keep_count") val keepCount: Int? = null,
    val title: String? = null
)

@kotlinx.serialization.Serializable
data class CompressSessionRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("focus_topic") val focusTopic: String? = null
)

@kotlinx.serialization.Serializable
data class UpdateSessionRequest(
    @SerialName("session_id") val sessionId: String,
    val workspace: String? = null,
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null
)

@kotlinx.serialization.Serializable
data class MoveSessionRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("project_id") val projectId: String? = null
)

@kotlinx.serialization.Serializable
data class ChatStartRequest(
    @SerialName("session_id") val sessionId: String,
    val message: String,
    val workspace: String? = null,
    val model: String? = null,
    @SerialName("model_provider") val modelProvider: String? = null,
    val profile: String? = null,
    @SerialName("explicit_model_pick") val explicitModelPick: Boolean? = null,
    val attachments: List<AttachmentUpload>? = null
)

@kotlinx.serialization.Serializable
data class AttachmentUpload(
    val filename: String? = null,
    val path: String? = null,
    val mime: String? = null
)

@kotlinx.serialization.Serializable
data class ChatSteerRequest(
    @SerialName("session_id") val sessionId: String,
    val text: String
)

@kotlinx.serialization.Serializable
data class DefaultModelRequest(val model: String)

@kotlinx.serialization.Serializable
data class SwitchProfileRequest(val name: String)

// ── Session Export/Import ──────────────────────────────────────

@kotlinx.serialization.Serializable
data class SessionExportResponse(
    val ok: Boolean? = null,
    val data: kotlinx.serialization.json.JsonElement? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class SessionImportRequest(
    val data: kotlinx.serialization.json.JsonElement,
    val workspace: String? = null
)

// ── Goal ──────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class GoalRequest(
    @SerialName("session_id") val sessionId: String,
    val goal: String
)

@kotlinx.serialization.Serializable
data class GoalResponse(
    val ok: Boolean? = null,
    val error: String? = null
)

// ── BTW ───────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class BtwRequest(
    @SerialName("session_id") val sessionId: String,
    val text: String
)

@kotlinx.serialization.Serializable
data class BtwResponse(
    val ok: Boolean? = null,
    @SerialName("stream_id") val streamId: String? = null,
    val error: String? = null
)

// ── Background Tasks ──────────────────────────────────────────

@kotlinx.serialization.Serializable
data class BackgroundStatusResponse(
    val active: Boolean? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("started_at") val startedAt: Double? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class BackgroundStartRequest(
    @SerialName("session_id") val sessionId: String,
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class BackgroundStartResponse(
    val ok: Boolean? = null,
    @SerialName("stream_id") val streamId: String? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class BackgroundStopResponse(
    val ok: Boolean? = null,
    val error: String? = null
)

// ── TTS ───────────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class TtsRequest(
    val text: String,
    val voice: String? = null
)

@kotlinx.serialization.Serializable
data class TtsResponse(
    val ok: Boolean? = null,
    val audio: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    val error: String? = null
)

// ── Profile Active ────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class ProfileActiveResponse(
    val active: String? = null,
    val profiles: List<com.hermex.app.data.model.ProfileSummary>? = null
)

@kotlinx.serialization.Serializable
data class ProfileActiveRequest(
    val name: String
)

// ── Metrics ───────────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class MetricsTrackRequest(
    val event: String,
    val properties: Map<String, kotlinx.serialization.json.JsonElement>? = null
)

@kotlinx.serialization.Serializable
data class MetricsTrackResponse(
    val ok: Boolean? = null
)

// ── Last Commit ───────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class LastCommitResponse(
    val sha: String? = null,
    val message: String? = null,
    @SerialName("committed_at") val committedAt: Double? = null
)

// ── Agent Restart ─────────────────────────────────────────────

@kotlinx.serialization.Serializable
data class AgentRestartRequest(
    val reason: String? = null
)

@kotlinx.serialization.Serializable
data class AgentRestartResponse(
    val ok: Boolean? = null,
    val error: String? = null
)

// ── Approval/Clarification Responses ──────────────────────────

@kotlinx.serialization.Serializable
data class ApprovalRespondRequest(
    @SerialName("session_id") val sessionId: String,
    val choice: String
)

@kotlinx.serialization.Serializable
data class ClarificationRespondRequest(
    @SerialName("session_id") val sessionId: String,
    val response: String
)
