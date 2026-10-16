package io.element.android.libraries.matrix.impl

import io.element.android.libraries.matrix.api.user.UserPresence
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun parsePresence(content: String): String? = parseUserPresence(content).presence

internal fun parseUserPresence(content: String): UserPresence {
    val data = Json.parseToJsonElement(content) as? JsonObject
    fun text(key: String) = (data?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content
    return UserPresence(
        presence = text("presence")?.takeIf { it == "online" || it == "unavailable" || it == "offline" },
        statusMessage = text("status_msg")?.takeIf { it.isNotBlank() },
    )
}
