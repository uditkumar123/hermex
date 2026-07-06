package com.hermex.app.data.repository

import android.content.Context
import com.hermex.app.data.api.HermesApi
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.api.*
import com.hermex.app.data.model.*
import com.hermex.app.data.offline.OfflineCache
import timber.log.Timber

class SessionRepository(
    private val serverUrl: String,
    private val context: Context? = null
) {

    private val api: HermesApi by lazy { RetrofitProvider.createApi(serverUrl, context) }
    private val offlineCache: OfflineCache? by lazy { context?.let { OfflineCache(it) } }

    suspend fun fetchSessions(): Result<List<SessionSummary>> {
        return try {
            val response = api.sessions()
            val sessions = response.sessions?.filter { it.archived != true } ?: emptyList()
            // Cache for offline use
            offlineCache?.cacheSessions(sessions, serverUrl)
            Result.success(sessions)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch sessions")
            Result.failure(e)
        }
    }

    suspend fun getCachedSessions(): List<SessionSummary> {
        return offlineCache?.getCachedSessionsForServer(serverUrl) ?: emptyList()
    }

    suspend fun isOfflineDataAvailable(): Boolean {
        return getCachedSessions().isNotEmpty()
    }

    suspend fun searchSessions(query: String, content: Boolean = true, depth: Int = 5): Result<List<SessionSummary>> {
        return try {
            val response = api.searchSessions(
                query = query,
                content = if (content) 1 else 0,
                depth = depth
            )
            Result.success(response.sessions ?: emptyList())
        } catch (e: Exception) {
            Timber.e(e, "Failed to search sessions")
            Result.failure(e)
        }
    }

    suspend fun fetchSession(
        sessionId: String,
        includeMessages: Boolean = true,
        messageLimit: Int = 50,
        messageBefore: Int? = null
    ): Result<SessionDetail> {
        return try {
            val response = api.session(
                sessionId = sessionId,
                includeMessages = if (includeMessages) 1 else 0,
                messageLimit = messageLimit,
                messageBefore = messageBefore
            )
            val session = response.session
            if (session != null) {
                Result.success(session)
            } else {
                Result.failure(Exception("Session not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch session: $sessionId")
            Result.failure(e)
        }
    }

    suspend fun createSession(
        workspace: String? = null,
        model: String? = null,
        modelProvider: String? = null,
        profile: String? = null
    ): Result<SessionDetail> {
        return try {
            val response = api.createSession(
                CreateSessionRequest(
                    workspace = workspace,
                    model = model,
                    modelProvider = modelProvider,
                    profile = profile
                )
            )
            val session = response.session
            if (session != null) {
                Result.success(session)
            } else {
                Result.failure(Exception("Failed to create session"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create session")
            Result.failure(e)
        }
    }

    suspend fun renameSession(sessionId: String, title: String): Result<SessionMutationResponse> {
        return try {
            val response = api.renameSession(RenameSessionRequest(sessionId, title))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename session")
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<SessionMutationResponse> {
        return try {
            val response = api.deleteSession(SessionIdRequest(sessionId))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session")
            Result.failure(e)
        }
    }

    suspend fun pinSession(sessionId: String, pinned: Boolean): Result<SessionMutationResponse> {
        return try {
            val response = api.pinSession(PinSessionRequest(sessionId, pinned))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to pin session")
            Result.failure(e)
        }
    }

    suspend fun archiveSession(sessionId: String, archived: Boolean): Result<SessionMutationResponse> {
        return try {
            val response = api.archiveSession(ArchiveSessionRequest(sessionId, archived))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to archive session")
            Result.failure(e)
        }
    }

    suspend fun moveSession(sessionId: String, projectId: String?): Result<SessionMutationResponse> {
        return try {
            val response = api.moveSession(MoveSessionRequest(sessionId, projectId))
            if (response.error != null) {
                Result.failure(Exception(response.error))
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to move session")
            Result.failure(e)
        }
    }

    suspend fun fetchSessionStatus(sessionId: String): Result<SessionStatusResponse> {
        return try {
            Result.success(api.sessionStatus(sessionId))
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch session status")
            Result.failure(e)
        }
    }

    suspend fun fetchWorkspaces(): Result<WorkspacesResponse> {
        return try {
            Result.success(api.workspaces())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch workspaces")
            Result.failure(e)
        }
    }

    suspend fun fetchProjects(): Result<ProjectsResponse> {
        return try {
            Result.success(api.projects())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch projects")
            Result.failure(e)
        }
    }

    suspend fun fetchProfiles(): Result<ProfilesResponse> {
        return try {
            Result.success(api.profiles())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch profiles")
            Result.failure(e)
        }
    }

    suspend fun switchProfile(name: String): Result<ProfileSwitchResponse> {
        return try {
            Result.success(api.switchProfile(SwitchProfileRequest(name)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to switch profile")
            Result.failure(e)
        }
    }

    suspend fun fetchModels(): Result<ModelsResponse> {
        return try {
            Result.success(api.models())
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch models")
            Result.failure(e)
        }
    }

    suspend fun undoSession(sessionId: String): Result<SessionMutationResponse> {
        return try {
            Result.success(api.undoSession(SessionIdRequest(sessionId)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to undo session")
            Result.failure(e)
        }
    }

    suspend fun retrySession(sessionId: String): Result<SessionMutationResponse> {
        return try {
            Result.success(api.retrySession(SessionIdRequest(sessionId)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to retry session")
            Result.failure(e)
        }
    }

    suspend fun compressSession(sessionId: String, focusTopic: String? = null): Result<SessionMutationResponse> {
        return try {
            Result.success(api.compressSession(CompressSessionRequest(sessionId, focusTopic)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to compress session")
            Result.failure(e)
        }
    }

    suspend fun branchSession(sessionId: String, keepCount: Int? = null, title: String? = null): Result<SessionMutationResponse> {
        return try {
            Result.success(api.branchSession(BranchSessionRequest(sessionId, keepCount, title)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to branch session")
            Result.failure(e)
        }
    }

    suspend fun updateSession(
        sessionId: String,
        model: String? = null,
        modelProvider: String? = null,
        workspace: String? = null
    ): Result<SessionResponse> {
        return try {
            Result.success(api.updateSession(UpdateSessionRequest(sessionId, workspace, model, modelProvider)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to update session")
            Result.failure(e)
        }
    }

    suspend fun sendBtw(sessionId: String, text: String): Result<BtwResponse> {
        return try {
            Result.success(api.btw(BtwRequest(sessionId, text)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to send btw")
            Result.failure(e)
        }
    }

    suspend fun setGoal(sessionId: String, goal: String): Result<GoalResponse> {
        return try {
            Result.success(api.setGoal(GoalRequest(sessionId, goal)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to set goal")
            Result.failure(e)
        }
    }

    suspend fun backgroundStart(sessionId: String, message: String? = null): Result<BackgroundStartResponse> {
        return try {
            Result.success(api.startBackground(BackgroundStartRequest(sessionId, message)))
        } catch (e: Exception) {
            Timber.e(e, "Failed to start background task")
            Result.failure(e)
        }
    }
}
