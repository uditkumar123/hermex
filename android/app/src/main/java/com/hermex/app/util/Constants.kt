package com.hermex.app.util

object Constants {
    const val DEFAULT_PORT = 8787
    const val API_PREFIX = "/api"
    const val HEALTH_PATH = "/health"

    object Auth {
        const val LOGIN_PATH = "$API_PREFIX/auth/login"
        const val LOGOUT_PATH = "$API_PREFIX/auth/logout"
        const val STATUS_PATH = "$API_PREFIX/auth/status"
    }

    object Sessions {
        const val SESSIONS_PATH = "$API_PREFIX/sessions"
        const val SESSION_PATH = "$API_PREFIX/session"
        const val SEARCH_PATH = "$API_PREFIX/sessions/search"
    }

    object Chat {
        const val START_PATH = "$API_PREFIX/chat/start"
        const val STREAM_PATH = "$API_PREFIX/chat/stream"
        const val CANCEL_PATH = "$API_PREFIX/chat/cancel"
        const val STEER_PATH = "$API_PREFIX/chat/steer"
        const val STREAM_STATUS_PATH = "$API_PREFIX/chat/stream/status"
    }

    object Models {
        const val MODELS_PATH = "$API_PREFIX/models"
        const val MODELS_LIVE_PATH = "$API_PREFIX/models/live"
        const val DEFAULT_MODEL_PATH = "$API_PREFIX/default-model"
    }

    object Workspaces {
        const val WORKSPACES_PATH = "$API_PREFIX/workspaces"
    }

    object Profiles {
        const val PROFILES_PATH = "$API_PREFIX/profiles"
        const val SWITCH_PATH = "$API_PREFIX/profile/switch"
    }

    object Settings {
        const val SETTINGS_PATH = "$API_PREFIX/settings"
    }
}
