package com.hermex.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Double.toRelativeTime(): String {
    val now = System.currentTimeMillis() / 1000.0
    val diff = now - this
    val seconds = diff.toLong()

    return when {
        seconds < 60 -> "just now"
        seconds < TimeUnit.HOURS.toSeconds(1) -> "${seconds / TimeUnit.MINUTES.toSeconds(1)}m ago"
        seconds < TimeUnit.DAYS.toSeconds(1) -> "${seconds / TimeUnit.HOURS.toSeconds(1)}h ago"
        seconds < TimeUnit.DAYS.toSeconds(2) -> "yesterday"
        seconds < TimeUnit.DAYS.toSeconds(7) -> "${seconds / TimeUnit.DAYS.toSeconds(1)}d ago"
        else -> {
            val date = Date((this * 1000).toLong())
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}

fun Double.toFullDate(): String {
    val date = Date((this * 1000).toLong())
    return SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)
}

fun String.nonEmpty(): String? {
    val trimmed = trim()
    return if (trimmed.isEmpty()) null else trimmed
}

fun String.Companion.emptyToNull(): String? = null
