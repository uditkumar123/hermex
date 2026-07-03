package com.hermex.app.data.api

import com.hermex.app.data.auth.CustomHeaderStore
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
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

    var onUnauthorized: (() -> Unit)? = null

    fun getOrCreate(baseUrl: String): Retrofit {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (normalizedUrl == currentBaseUrl && currentRetrofit != null) {
            return currentRetrofit!!
        }

        val cookieJar = PersistentCookieJar()

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
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
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

    fun createApi(baseUrl: String): HermesApi {
        return getOrCreate(baseUrl).create(HermesApi::class.java)
    }

    fun createOkHttpClient(): OkHttpClient {
        val cookieJar = PersistentCookieJar()
        val headerInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            CustomHeaderStore.snapshot().forEach { header ->
                builder.addHeader(header.name, header.value)
            }
            chain.proceed(builder.build())
        }

        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(headerInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE
            .build()
    }

    fun invalidate() {
        currentBaseUrl = null
        currentRetrofit = null
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }
}

class PersistentCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore.getOrPut(host) { mutableListOf() }.apply {
            removeAll { existing -> cookies.any { it.name == existing.name } }
            addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        return cookieStore[host]?.filter { cookie ->
            !cookie.hasExpired()
        } ?: emptyList()
    }

    fun clearAll() {
        cookieStore.clear()
    }

    fun clearForHost(host: String) {
        cookieStore.remove(host)
    }

    private fun Cookie.hasExpired(): Boolean {
        return expiresAt < System.currentTimeMillis()
    }
}
