package io.element.android.libraries.matrix.api.user

/** Presence and optional status message reported by the homeserver. */
data class UserPresence(val presence: String?, val statusMessage: String? = null)
