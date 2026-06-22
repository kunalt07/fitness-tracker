package com.example.fitness_tracker.ai

import com.google.firebase.ai.type.ContentBlockedException
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Maps a Firebase AI exception (or any [Throwable] that bubbles up from a
 * generateContent call) into a short, human-friendly message suitable for
 * a snackbar or inline error label.
 *
 * The raw upstream messages are useful in logs but unfriendly in the UI —
 * e.g. "ServerException: This model is currently experiencing high demand"
 * is better surfaced as "Gemini is busy — try again in a moment."
 */
fun friendlyAiError(e: Throwable): String = when (e) {
    is ServerException -> "Gemini is busy right now. Try again in a moment."
    is QuotaExceededException -> "AI quota exceeded for today. Try again later."
    is RequestTimeoutException -> "AI took too long to respond. Try again."
    is PromptBlockedException,
    is ContentBlockedException -> "Couldn't process that prompt — try rephrasing."
    is InvalidAPIKeyException -> "AI is misconfigured. Check google-services.json."
    is UnsupportedUserLocationException -> "AI isn't available in your region yet."
    is UnknownHostException -> "No internet connection."
    is IOException -> "Network hiccup. Check your connection and try again."
    else -> e.localizedMessage?.takeIf { it.isNotBlank() }
        ?: "Something went wrong with the AI request."
}
