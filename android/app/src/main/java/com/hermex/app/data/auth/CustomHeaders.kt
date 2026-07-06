package com.hermex.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.hermex.app.data.api.RetrofitProvider
import com.hermex.app.data.model.CustomHeader
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

object CustomHeaderStore {
    private var headers: List<CustomHeader> = emptyList()
    private var serverUrl: String? = null

    private val json = Json { ignoreUnknownKeys = true }

    fun configure(serverUrl: String, context: Context) {
        this.serverUrl = serverUrl
        headers = loadHeaders(serverUrl, context)
    }

    fun snapshot(): List<CustomHeader> = headers

    fun addHeader(name: String, value: String, context: Context) {
        val header = CustomHeader(name = name, value = value)
        headers = headers + header
        saveHeaders(context)
    }

    fun removeHeader(id: String, context: Context) {
        headers = headers.filter { it.id != id }
        saveHeaders(context)
    }

    fun updateHeader(id: String, name: String, value: String, context: Context) {
        headers = headers.map {
            if (it.id == id) it.copy(name = name, value = value) else it
        }
        saveHeaders(context)
    }

    fun clear() {
        headers = emptyList()
        serverUrl = null
    }

    private fun loadHeaders(serverUrl: String, context: Context): List<CustomHeader> {
        return try {
            val prefs = getPrefs(context) ?: return emptyList()
            val key = "custom_headers_${serverUrl.hashCode()}"
            val jsonStr = prefs.getString(key, null)
                ?: legacyPrefs(context).getString(key, null)?.also { legacy ->
                    prefs.edit().putString(key, legacy).apply()
                }
                ?: return emptyList()
            json.decodeFromString<List<CustomHeader>>(jsonStr)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load custom headers")
            emptyList()
        }
    }

    private fun saveHeaders(context: Context) {
        val url = serverUrl ?: return
        try {
            val prefs = getPrefs(context) ?: return
            val key = "custom_headers_${url.hashCode()}"
            prefs.edit().putString(key, json.encodeToString(headers)).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save custom headers")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "hermex_headers_encrypted",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Timber.w(e, "EncryptedSharedPreferences unavailable, falling back to plain storage")
            context.getSharedPreferences("hermex_headers", Context.MODE_PRIVATE)
        }
    }

    private fun legacyPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("hermex_headers", Context.MODE_PRIVATE)
    }
}
