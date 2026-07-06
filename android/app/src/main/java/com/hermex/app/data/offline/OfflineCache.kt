package com.hermex.app.data.offline

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hermex.app.data.model.SessionSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions_cache")

class OfflineCache(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private object Keys {
        val SESSIONS = stringPreferencesKey("cached_sessions")
        val SERVER_URL = stringPreferencesKey("cached_server_url")
        val LAST_UPDATED = longPreferencesKey("last_updated")
    }

    suspend fun cacheSessions(sessions: List<SessionSummary>, serverUrl: String) {
        try {
            context.sessionDataStore.edit { prefs ->
                prefs[Keys.SESSIONS] = json.encodeToString(sessions)
                prefs[Keys.SERVER_URL] = serverUrl
                prefs[Keys.LAST_UPDATED] = System.currentTimeMillis()
            }
            Timber.d("Cached ${sessions.size} sessions")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache sessions")
        }
    }

    suspend fun getCachedSessions(): Pair<List<SessionSummary>, String?> {
        return try {
            val prefs = context.sessionDataStore.data.first()
            val sessionsJson = prefs[Keys.SESSIONS] ?: return Pair(emptyList(), null)
            val serverUrl = prefs[Keys.SERVER_URL]
            val sessions = json.decodeFromString<List<SessionSummary>>(sessionsJson)
            Pair(sessions, serverUrl)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get cached sessions")
            Pair(emptyList(), null)
        }
    }

    suspend fun getCachedSessionsForServer(serverUrl: String): List<SessionSummary> {
        val (sessions, cachedServerUrl) = getCachedSessions()
        if (cachedServerUrl == null) return emptyList()
        return if (sameServer(cachedServerUrl, serverUrl)) sessions else emptyList()
    }

    suspend fun getLastUpdated(): Long {
        return try {
            val prefs = context.sessionDataStore.data.first()
            prefs[Keys.LAST_UPDATED] ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun clearCache() {
        try {
            context.sessionDataStore.edit { it.clear() }
            Timber.d("Session cache cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear session cache")
        }
    }

    private fun sameServer(a: String, b: String): Boolean {
        return a.trim().trimEnd('/') == b.trim().trimEnd('/')
    }
}
