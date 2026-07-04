package com.hermex.app.data.repository

import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.api.SSEClient
import com.hermex.app.data.api.*
import com.hermex.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class ChatRepository(
    private val serverUrl: String,
    private val context: android.content.Context? = null
) {

    private val api by lazy { RetrofitProvider.createApi(serverUrl, context) }
    private val sseClient by lazy { SSEClient(RetrofitProvider.createOkHttpClient(context)) }

    suspend fun startChat(
        sessionId: String,
        message: String,
        workspace: String? = null,
        model: String? = null,
        modelProvider: String? = null,
        profile: String? = null,
        explicitModelPick: Boolean? = null,
        attachments: List<AttachmentUpload>? = null
    ): Result<ChatStartResponse> {
        return try {
            val response = api.startChat(
                ChatStartRequest(
                    sessionId = sessionId,
                    message = message,
                    workspace = workspace,
                    model = model,
                    modelProvider = modelProvider,
                    profile = profile,
                    explicitModelPick = explicitModelPick,
                    attachments = attachments
                )
            )
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start chat")
            Result.failure(e)
        }
    }

    fun streamChat(streamId: String, afterSeq: Int? = null): Flow<SSEStreamEvent> {
        val baseUrl = serverUrl.trimEnd('/')
        val url = buildString {
            append("$baseUrl/api/chat/stream?stream_id=$streamId")
            if (afterSeq != null) {
                append("&replay=1&after_seq=$afterSeq")
            }
        }
        return sseClient.stream(url)
    }

    suspend fun cancelChat(streamId: String): Result<ChatCancelResponse> {
        return try {
            Result.success(api.cancelChat(streamId))
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel chat")
            Result.failure(e)
        }
    }

    suspend fun steerChat(sessionId: String, text: String): Result<ChatSteerResponse> {
        return try {
            val response = api.steerChat(ChatSteerRequest(sessionId, text))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to steer chat")
            Result.failure(e)
        }
    }

    suspend fun checkStreamStatus(streamId: String): Result<ChatStreamStatusResponse> {
        return try {
            Result.success(api.chatStreamStatus(streamId))
        } catch (e: Exception) {
            Timber.e(e, "Failed to check stream status")
            Result.failure(e)
        }
    }

    suspend fun respondToApproval(sessionId: String, choice: String): Result<ApprovalRespondResponse> {
        return try {
            val response = api.respondToApproval(ApprovalRespondRequest(sessionId, choice))
            if (response.ok != true) {
                Result.failure(Exception("Approval response failed"))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to respond to approval")
            Result.failure(e)
        }
    }

    suspend fun respondToClarification(sessionId: String, response: String): Result<ClarificationRespondResponse> {
        return try {
            val response = api.respondToClarification(ClarificationRespondRequest(sessionId, response))
            if (response.ok != true) {
                Result.failure(Exception("Clarification response failed"))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to respond to clarification")
            Result.failure(e)
        }
    }

    fun stop() {
        sseClient.stop()
    }
}
