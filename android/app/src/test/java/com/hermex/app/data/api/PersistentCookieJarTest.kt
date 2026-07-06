package com.hermex.app.data.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PersistentCookieJarTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `constructor does not throw with valid context`() {
        val jar = PersistentCookieJar(context)
        assertNotNull(jar)
    }

    @Test
    fun `constructor does not throw with null context`() {
        val jar = PersistentCookieJar(null)
        assertNotNull(jar)
    }

    @Test
    fun `save and load cookie round-trips correctly`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val url = "https://example.com".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .path("/")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie))

        val loaded = jar.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("session", loaded[0].name)
        assertEquals("abc123", loaded[0].value)
    }

    @Test
    fun `load returns empty list for unknown host`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val loaded = jar.loadForRequest("https://unknown.example.com".toHttpUrl())
        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `expired cookie is filtered out`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val url = "https://example.com".toHttpUrl()
        val expiredCookie = Cookie.Builder()
            .name("old")
            .value("expired")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() - 3600_000)
            .build()
        val validCookie = Cookie.Builder()
            .name("fresh")
            .value("valid")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(expiredCookie, validCookie))

        val loaded = jar.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("fresh", loaded[0].name)
    }

    @Test
    fun `save replaces cookie with same name`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val url = "https://example.com".toHttpUrl()
        val cookie1 = Cookie.Builder()
            .name("session")
            .value("first")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()
        val cookie2 = Cookie.Builder()
            .name("session")
            .value("second")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie1))
        jar.saveFromResponse(url, listOf(cookie2))

        val loaded = jar.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("second", loaded[0].value)
    }

    @Test
    fun `clearAll removes all cookies`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val url = "https://example.com".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc123")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie))
        assertEquals(1, jar.loadForRequest(url).size)

        jar.clearAll()
        assertTrue(jar.loadForRequest(url).isEmpty())
    }

    @Test
    fun `clearForHost removes only that host`() {
        val jar = PersistentCookieJar(context)
        jar.clearAll()

        val urlA = "https://a.example.com".toHttpUrl()
        val urlB = "https://b.example.com".toHttpUrl()
        val cookieA = Cookie.Builder()
            .name("session")
            .value("aaa")
            .domain("a.example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()
        val cookieB = Cookie.Builder()
            .name("session")
            .value("bbb")
            .domain("b.example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(urlA, listOf(cookieA))
        jar.saveFromResponse(urlB, listOf(cookieB))

        jar.clearForHost("a.example.com")

        assertTrue(jar.loadForRequest(urlA).isEmpty())
        assertEquals(1, jar.loadForRequest(urlB).size)
        assertEquals("bbb", jar.loadForRequest(urlB)[0].value)
    }

    @Test
    fun `cookies persist across instances`() {
        val jar1 = PersistentCookieJar(context)
        jar1.clearAll()

        val url = "https://example.com".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("persistent123")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar1.saveFromResponse(url, listOf(cookie))

        val jar2 = PersistentCookieJar(context)
        val loaded = jar2.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertEquals("persistent123", loaded[0].value)

        jar2.clearAll()
    }

    @Test
    fun `secure and httpOnly attributes persist across instances`() {
        val jar1 = PersistentCookieJar(context)
        jar1.clearAll()

        val url = "https://example.com".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("persistent123")
            .hostOnlyDomain("example.com")
            .path("/")
            .secure()
            .httpOnly()
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar1.saveFromResponse(url, listOf(cookie))

        val jar2 = PersistentCookieJar(context)
        val loaded = jar2.loadForRequest(url)
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].secure)
        assertTrue(loaded[0].httpOnly)
        assertTrue(loaded[0].hostOnly)
        assertTrue(jar2.loadForRequest("http://example.com".toHttpUrl()).isEmpty())

        jar2.clearAll()
    }

    @Test
    fun `loadForRequest with null context returns empty list`() {
        val jar = PersistentCookieJar(null)
        val loaded = jar.loadForRequest("https://example.com".toHttpUrl())
        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `saveFromResponse with null context does not throw`() {
        val jar = PersistentCookieJar(null)
        val url = "https://example.com".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("session")
            .value("abc")
            .domain("example.com")
            .expiresAt(System.currentTimeMillis() + 3600_000)
            .build()

        jar.saveFromResponse(url, listOf(cookie))
    }
}
