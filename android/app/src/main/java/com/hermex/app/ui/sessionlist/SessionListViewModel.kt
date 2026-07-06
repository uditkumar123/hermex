package com.hermex.app.ui.sessionlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import com.hermex.app.data.model.ProfileSummary
import com.hermex.app.data.model.ProjectSummary
import com.hermex.app.data.model.SessionSummary
import com.hermex.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SessionListUiState(
    val sessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCreatingSession: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val profiles: List<ProfileSummary> = emptyList(),
    val activeProfile: String? = null,
    val projects: List<ProjectSummary> = emptyList(),
    val selectedProjectId: String? = null,
    val isOfflineMode: Boolean = false
)

class SessionListViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager.getInstance(application)

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    private val repository: SessionRepository? by lazy {
        val url = (authManager.state.value as? AuthState.LoggedIn)?.serverUrl
        url?.let { SessionRepository(it, getApplication()) }
    }

    init {
        authManager.initialize()
    }

    fun loadSessions() {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repo.fetchSessions().fold(
                onSuccess = { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions,
                            isLoading = false,
                            isRefreshing = false,
                            isOfflineMode = false
                        )
                    }
                },
                onFailure = { e ->
                    // Try offline cache
                    val cachedSessions = repo.getCachedSessions()
                    if (cachedSessions.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                sessions = cachedSessions,
                                isLoading = false,
                                isRefreshing = false,
                                isOfflineMode = true,
                                errorMessage = "Offline mode - showing cached data"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = e.message
                            )
                        }
                    }
                }
            )
        }
    }

    fun refresh() {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            repo.fetchSessions().fold(
                onSuccess = { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchSessionsRemote(query: String) {
        val repo = repository ?: return
        if (query.isBlank()) {
            loadSessions()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repo.searchSessions(query).fold(
                onSuccess = { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    fun createSession(onCreated: (String) -> Unit) {
        val repo = repository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSession = true) }
            repo.createSession().fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(isCreatingSession = false)
                    }
                    val sessionId = session.sessionId
                    if (sessionId != null) {
                        onCreated(sessionId)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCreatingSession = false,
                            errorMessage = e.message
                        )
                    }
                }
            )
        }
    }

    fun pinSession(session: SessionSummary) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.pinSession(sessionId, session.pinned != true).fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun archiveSession(session: SessionSummary) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.archiveSession(sessionId, true).fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun deleteSession(session: SessionSummary) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.deleteSession(sessionId).fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun filteredSessions(): List<SessionSummary> {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val projectId = state.selectedProjectId

        var sessions = state.sessions

        // Filter by project if selected
        if (projectId != null) {
            sessions = sessions.filter { it.projectId == projectId }
        }

        // Filter by search query
        if (query.isEmpty()) return sessions

        return sessions.filter { session ->
            listOfNotNull(
                session.title,
                session.workspace,
                session.model,
                session.modelProvider,
                session.profile,
                session.sourceLabel
            ).any { it?.lowercase()?.contains(query) == true }
        }
    }

    fun sectionedSessions(): List<SessionSection> {
        val sessions = filteredSessions()
        val pinned = sessions.filter { it.pinned == true }
        val unpinned = sessions.filter { it.pinned != true }

        val today = mutableListOf<SessionSummary>()
        val yesterday = mutableListOf<SessionSummary>()
        val earlier = mutableListOf<SessionSummary>()

        val now = System.currentTimeMillis() / 1000.0
        val todayStart = now - (now % 86400)
        val yesterdayStart = todayStart - 86400

        unpinned.forEach { session ->
            val ts = session.effectiveTimestamp
            when {
                ts >= todayStart -> today.add(session)
                ts >= yesterdayStart -> yesterday.add(session)
                else -> earlier.add(session)
            }
        }

        return buildList {
            if (pinned.isNotEmpty()) add(SessionSection("Pinned", pinned))
            if (today.isNotEmpty()) add(SessionSection("Today", today))
            if (yesterday.isNotEmpty()) add(SessionSection("Yesterday", yesterday))
            if (earlier.isNotEmpty()) add(SessionSection("Earlier", earlier))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun renameSession(session: SessionSummary, newTitle: String) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.renameSession(sessionId, newTitle).fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun duplicateSession(session: SessionSummary) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.branchSession(sessionId).fold(
                onSuccess = { response ->
                    refresh()
                    val newSessionId = response.sessionId ?: response.session?.sessionId
                    if (newSessionId != null) {
                        // TODO: Navigate to new session
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun loadProfiles() {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.fetchProfiles().fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            profiles = response.profiles ?: emptyList(),
                            activeProfile = response.active
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun switchProfile(profileName: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.switchProfile(profileName).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            profiles = response.profiles ?: emptyList(),
                            activeProfile = response.active
                        )
                    }
                    refresh()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun loadProjects() {
        val repo = repository ?: return
        viewModelScope.launch {
            repo.fetchProjects().fold(
                onSuccess = { response ->
                    _uiState.update { it.copy(projects = response.projects ?: emptyList()) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }

    fun filterByProject(projectId: String?) {
        _uiState.update { it.copy(selectedProjectId = projectId) }
    }

    fun moveToProject(session: SessionSummary, projectId: String?) {
        val repo = repository ?: return
        val sessionId = session.sessionId ?: return
        viewModelScope.launch {
            repo.moveSession(sessionId, projectId).fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            )
        }
    }
}

data class SessionSection(
    val title: String,
    val sessions: List<SessionSummary>
)
