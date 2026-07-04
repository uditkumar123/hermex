package com.hermex.app.ui.workspace

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class MemoryUiState(
    val notes: String? = null,
    val profile: String? = null,
    val sessionNotes: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0
)

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    fun loadMemory() {
        val url = getServerUrl() ?: return
        val api = RetrofitProvider.createApi(url)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.memory()
                _uiState.update { it.copy(notes = response.notes, profile = response.profile, sessionNotes = response.sessionNotes, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load memory", isLoading = false) }
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getServerUrl(): String? {
        val context = getApplication<Application>()
        return (AuthManager.getInstance(context).state.value as? AuthState.LoggedIn)?.serverUrl
    }
}
