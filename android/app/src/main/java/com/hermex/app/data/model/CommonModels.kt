package com.hermex.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JSONValue(
    val value: JsonElement? = null
)

sealed class APIError : Exception() {
    data object InvalidServerURL : APIError() {
        override val message: String get() = "Invalid server URL"
    }
    data class Network(override val cause: Throwable) : APIError() {
        override val message: String get() = "Network error: ${cause.message}"
    }
    data class Http(val statusCode: Int, val body: String? = null) : APIError() {
        override val message: String get() = "HTTP $statusCode: ${body ?: "Unknown error"}"
    }
    data class Decoding(override val cause: Throwable) : APIError() {
        override val message: String get() = "Decoding error: ${cause.message}"
    }
    data object Unauthorized : APIError() {
        override val message: String get() = "Unauthorized"
    }
}

data class CustomHeader(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val value: String
)
