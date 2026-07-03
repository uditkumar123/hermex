package com.hermex.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermex.app.data.auth.AuthManager
import com.hermex.app.data.auth.AuthState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = AuthManager.getInstance(application)

    val authState = authManager.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AuthState.Unconfigured
    )

    val isLoading = authManager.isLoading.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val error = authManager.error.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        authManager.initialize()
    }

    fun login(serverUrl: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authManager.login(serverUrl, password)
            result.fold(
                onSuccess = { onResult(true) },
                onFailure = { onResult(false) }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

    fun clearError() {
        authManager.clearError()
    }
}
