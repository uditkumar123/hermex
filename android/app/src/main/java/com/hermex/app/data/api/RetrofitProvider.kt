package com.hermex.app.data.api

import android.util.Log
import com.hermex.app.data.auth.CustomHeaderStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import java.net.URI
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private var currentBaseUrl: String? = null
    private var currentRetrofit: Retrofit? = null
    private var cookieJar: PersistentCookieJar? = null

    var onUnauthorized: (() -> Unit)? = null

    fun clearCookiesForHost(host: String) {
        cookieJar?.clearForHost(host)
    }

    private fun ensureCookieJar(context: android.content.Context?): PersistentCookieJar {
        if (cookieJar == null) {
            cookieJar = PersistentCookieJar(context)
        }
        return cookieJar!!
    }

    fun getOrCreate(baseUrl: String, context: android.content.Context? = null): Retrofit {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (normalizedUrl == currentBaseUrl && currentRetrofit != null) {
            return currentRetrofit!!
        }

        val jar = ensureCookieJar(context)

        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()

            CustomHeaderStore.snapshot().forEach { header ->
                builder.addHeader(header.name, header.value)
            }

            builder.header("Accept", "application/json")
            builder.header("Cache-Control", "no-cache")

            chain.proceed(builder.build())
        }

        val authInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                onUnauthorized?.invoke()
            }
            response
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (context != null && (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(jar)
            .addInterceptor(headerInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        currentBaseUrl = normalizedUrl
        currentRetrofit = retrofit
        return retrofit
    }

    fun createApi(baseUrl: String, context: android.content.Context? = null): HermesApi {
        return getOrCreate(baseUrl, context).create(HermesApi::class.java)
    }

    fun createOkHttpClient(context: android.content.Context? = null): OkHttpClient {
        val jar = ensureCookieJar(context)
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            CustomHeaderStore.snapshot().forEach { header ->
                builder.addHeader(header.name, header.value)
            }
            chain.proceed(builder.build())
        }

        return OkHttpClient.Builder()
            .cookieJar(jar)
            .addInterceptor(headerInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    fun invalidate() {
        currentBaseUrl = null
        currentRetrofit = null
        cookieJar?.clearAll()
    }

    fun warmupSession(baseUrl: String, context: android.content.Context? = null) {
        try {
            val normalizedUrl = normalizeUrl(baseUrl)
            val client = getOrCreate(baseUrl, context).callFactory() as OkHttpClient
            val request = okhttp3.Request.Builder()
                .url(normalizedUrl)
                .get()
                .header("Accept", "text/html,application/json")
                .build()
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.w("Hermex", "Session warmup failed: ${e.message}")
        }
    }

    fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        if (normalized.startsWith("http://")) {
            val host = try {
                java.net.URI(normalized).host
            } catch (e: Exception) { Log.w("Hermex", "Failed to parse URL host: ${e.message}"); null }
            if (host != null && !isLocalAddress(host)) {
                Log.w("Hermex", "Connecting via unencrypted HTTP to non-local server: $host")
            }
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }

    private fun isLocalAddress(host: String): Boolean {
        if (host == "localhost" || host.startsWith("127.")) return true
        if (host.startsWith("10.")) return true
        if (host.startsWith("192.168.")) return true
        if (host.startsWith("172.")) {
            val second = host.substringAfter(".").substringBefore(".").toIntOrNull() ?: return false
            if (second in 16..31) return true
        }
        if (host.startsWith("100.")) {
            val second = host.substringAfter(".").substringBefore(".").toIntOrNull() ?: return false
            if (second in 64..127) return true
        }
        if (host.endsWith(".local")) return true
        return false
    }
}

class PersistentCookieJar(private val context: android.content.Context? = null) : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private val prefs by lazy {
        context?.let { ctx ->
            try {
                val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(
                    androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
                )
                androidx.security.crypto.EncryptedSharedPreferences.create(
                    "hermex_cookies_encrypted",
                    masterKeyAlias,
                    ctx,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Throwable) {
                Log.w("Hermex", "EncryptedSharedPreferences unavailable, falling back to plain storage", e)
                ctx.getSharedPreferences("hermex_cookies", android.content.Context.MODE_PRIVATE)
            }
        }
    }

    init {
        context?.let { loadFromStorage() }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore.getOrPut(host) { mutableListOf() }.apply {
            removeAll { existing -> cookies.any { it.name == existing.name } }
            addAll(cookies)
        }
        persistToStorage()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        return cookieStore[host]?.filter { cookie ->
            !cookie.hasExpired()
        } ?: emptyList()
    }

    fun clearAll() {
        cookieStore.clear()
        persistToStorage()
    }

    fun clearForHost(host: String) {
        cookieStore.remove(host)
        persistToStorage()
    }

    private fun Cookie.hasExpired(): Boolean {
        return expiresAt < System.currentTimeMillis()
    }

    private fun persistToStorage() {
        val p = prefs ?: return
        try {
            val editor = p.edit()
            val hosts = cookieStore.keys.toSet()
            editor.putStringSet("hosts", hosts)
            cookieStore.forEach { (host, cookies) ->
                val serialized = cookies.joinToString("\n") { cookie ->
                    "${cookie.name}\t${cookie.value}\t${cookie.expiresAt}\t${cookie.domain}\t${cookie.path}"
                }
                editor.putString("cookies_$host", serialized)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w("Hermex", "Failed to persist cookies: ${e.message}")
        }
    }

    private fun loadFromStorage() {
        val p = prefs ?: return
        try {
            val hosts = p.getStringSet("hosts", emptySet()) ?: emptySet()
            hosts.forEach { host ->
                val serialized = p.getString("cookies_$host", null) ?: return@forEach
                val cookies = serialized.split("\n").mapNotNull { line ->
                    val parts = line.split("\t")
                    if (parts.size >= 5) {
                        try {
                            Cookie.Builder()
                                .name(parts[0])
                                .value(parts[1])
                                .expiresAt(parts[2].toLong())
                                .domain(parts[3])
                                .let { if (parts[4].isNotEmpty()) it.path(parts[4]) else it }
                                .build()
                        } catch (e: Exception) { Log.w("Hermex", "Failed to parse cookie: ${e.message}"); null }
                    } else null
                }
                cookieStore[host] = cookies.toMutableList()
            }
        } catch (e: Exception) {
            Log.w("Hermex", "Failed to load cookies: ${e.message}")
        }
    }
}
