package com.hermex.app.data.api

import com.hermex.app.data.auth.SessionExpiredMessage
import com.hermex.app.data.model.ConnectionState
import com.hermex.app.data.model.SSEEvent
import com.hermex.app.data.model.SSEStreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SSEClient(
    private val okHttpClient: OkHttpClient = RetrofitProvider.createOkHttpClient()
) {
    private var currentEventSource: EventSource? = null
    private val backoffDelays = listOf(1000L, 2000L, 4000L, 8000L, 30000L)

    fun stream(url: String): Flow<SSEStreamEvent> = callbackFlow {
        var lastEventId: String? = null
        var retryCount = 0
        var shouldReconnect = true

        suspend fun openConnection(): Boolean {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache, no-transform")
                .header("Accept-Encoding", "identity")
            val currentLastId = lastEventId
            if (currentLastId != null) {
                requestBuilder.header("Last-Event-ID", currentLastId)
            }

            return suspendCoroutine { cont ->
                val factory = EventSources.createFactory(okHttpClient)
                val listener = object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        if (id != null) lastEventId = id
                        val eventType = type ?: "message"
                        val event = SseEventDecoder.decode(eventType, data)
                        trySend(SSEStreamEvent.Event(event))
                    }

                    override fun onClosed(eventSource: EventSource) {
                        shouldReconnect = false
                        cont.resume(true)
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        Timber.e(t, "SSE connection failure")
                        if (response?.code == 401) {
                            shouldReconnect = false
                            trySend(SSEStreamEvent.Event(SSEEvent.TransportError(SessionExpiredMessage)))
                            cont.resume(false)
                            return
                        }
                        retryCount++
                        if (retryCount >= backoffDelays.size) {
                            shouldReconnect = false
                        }
                        cont.resume(false)
                    }
                }
                currentEventSource = factory.newEventSource(requestBuilder.build(), listener)
            }
        }

        trySend(SSEStreamEvent.StateChange(ConnectionState.Connected))
        val firstResult = openConnection()

        if (firstResult && shouldReconnect) {
            return@callbackFlow
        }

        while (isActive && shouldReconnect && retryCount < backoffDelays.size) {
            trySend(SSEStreamEvent.StateChange(ConnectionState.Reconnecting))
            delay(backoffDelays[retryCount - 1])

            trySend(SSEStreamEvent.StateChange(ConnectionState.Connected))
            val result = openConnection()
            if (result && shouldReconnect) {
                return@callbackFlow
            }
        }

        if (retryCount >= backoffDelays.size) {
            trySend(SSEStreamEvent.StateChange(ConnectionState.Disconnected))
            trySend(SSEStreamEvent.Event(SSEEvent.TransportError("Connection lost. Max retries exceeded.")))
        }

        awaitClose {
            currentEventSource?.cancel()
            currentEventSource = null
        }
    }

    fun stop() {
        currentEventSource?.cancel()
        currentEventSource = null
    }
}
