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
import kotlinx.serialization.json.*
import timber.log.Timber

data class SkillsUiState(
    val skills: List<SkillItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedSkill: SkillItem? = null,
    val skillContent: String? = null,
    val contentLoading: Boolean = false
)

data class SkillItem(val name: String, val description: String? = null)

class SkillsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SkillsUiState())
    val uiState: StateFlow<SkillsUiState> = _uiState.asStateFlow()

    fun loadSkills() {
        val url = getServerUrl() ?: return
        val api = RetrofitProvider.createApi(url)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.skills()
                val skills = response.skills?.mapNotNull { skill ->
                    try {
                        val obj = skill.jsonObject
                        SkillItem(
                            name = obj["name"]?.jsonPrimitive?.content ?: "",
                            description = obj["description"]?.jsonPrimitive?.content
                        )
                    } catch (e: Exception) { Timber.w(e, "Failed to parse skill"); null }
                } ?: emptyList()
                _uiState.update { it.copy(skills = skills, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load skills", isLoading = false) }
            }
        }
    }

    fun loadSkillContent(name: String) {
        val url = getServerUrl() ?: return
        val api = RetrofitProvider.createApi(url)
        viewModelScope.launch {
            _uiState.update { it.copy(contentLoading = true, errorMessage = null, skillContent = null) }
            try {
                val response = api.skillContent(name)
                _uiState.update { it.copy(skillContent = response.content, contentLoading = false) }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load skill content: $name")
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to load skill content", contentLoading = false) }
            }
        }
    }

    fun selectSkill(skill: SkillItem?) {
        _uiState.update { it.copy(selectedSkill = skill, skillContent = null, errorMessage = null) }
        if (skill != null) loadSkillContent(skill.name)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun getServerUrl(): String? {
        val context = getApplication<Application>()
        return (AuthManager.getInstance(context).state.value as? AuthState.LoggedIn)?.serverUrl
    }
}
