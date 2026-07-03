package com.hermex.app.data.api

import com.hermex.app.data.model.SSEEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber

class SSEClient(
    private val okHttpClient: OkHttpClient = RetrofitProvider.createOkHttpClient()
) {
    private var currentEventSource: EventSource? = null

    fun stream(url: String): Flow<SSEEvent> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache, no-transform")
            .header("Accept-Encoding", "identity")
            .build()

        val factory = EventSources.createFactory(okHttpClient)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                val eventType = type ?: "message"
                val event = SseEventDecoder.decode(eventType, data)
                trySend(event)
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SSEEvent.StreamEnd)
                close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val message = when {
                    t != null -> t.message ?: "SSE connection failed"
                    response != null -> "SSE error: ${response.code}"
                    else -> "SSE connection failed"
                }
                Timber.e(t, "SSE failure")
                trySend(SSEEvent.TransportError(message))
                close()
            }
        }

        currentEventSource = factory.newEventSource(request, listener)

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
