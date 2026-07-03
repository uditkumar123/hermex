package com.hermex.app.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class ServerAccount(
    val id: String,
    val urlString: String,
    val displayName: String? = null,
    val initials: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object ServerRegistry {
    private const val PREFS_NAME = "hermex_servers"
    private const val KEY_SERVERS = "servers"
    private const val KEY_ACTIVE_ID = "active_server_id"

    private val json = Json { ignoreUnknownKeys = true }

    private var _servers: List<ServerAccount> = emptyList()
    val servers: List<ServerAccount> get() = _servers

    private var _activeServerId: String? = null
    val activeServerId: String? get() = _activeServerId

    fun initialize(context: Context) {
        loadFromStorage(context)
    }

    fun addServer(url: String, displayName: String?, context: Context): ServerAccount {
        val account = ServerAccount(
            id = url.hashCode().toString(),
            urlString = url,
            displayName = displayName,
            initials = extractInitials(displayName ?: url)
        )
        _servers = _servers.filter { it.urlString != url } + account
        _activeServerId = account.id
        saveToStorage(context)
        return account
    }

    fun removeServer(id: String, context: Context) {
        _servers = _servers.filter { it.id != id }
        if (_activeServerId == id) {
            _activeServerId = _servers.firstOrNull()?.id
        }
        saveToStorage(context)
    }

    fun switchToServer(id: String, context: Context) {
        if (_servers.any { it.id == id }) {
            _activeServerId = id
            saveToStorage(context)
        }
    }

    fun activeServer(): ServerAccount? {
        return _servers.find { it.id == _activeServerId }
    }

    fun getServer(id: String): ServerAccount? {
        return _servers.find { it.id == id }
    }

    fun getServerByUrl(url: String): ServerAccount? {
        return _servers.find { it.urlString == url }
    }

    private fun loadFromStorage(context: Context) {
        try {
            val prefs = getPrefs(context)
            val serversJson = prefs.getString(KEY_SERVERS, null)
            _servers = if (serversJson != null) {
                json.decodeFromString<List<ServerAccount>>(serversJson)
            } else {
                emptyList()
            }
            _activeServerId = prefs.getString(KEY_ACTIVE_ID, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load server registry")
            _servers = emptyList()
            _activeServerId = null
        }
    }

    private fun saveToStorage(context: Context) {
        try {
            val prefs = getPrefs(context)
            prefs.edit()
                .putString(KEY_SERVERS, json.encodeToString(_servers))
                .putString(KEY_ACTIVE_ID, _activeServerId)
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save server registry")
        }
    }

    private fun getPrefs(context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun extractInitials(input: String): String {
        val words = input.trim().split("\\s+".toRegex()).take(2)
        return words.mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    }
}
