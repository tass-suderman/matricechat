package io.element.android.libraries.matrix.impl

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun parsePresence(content: String): String? {
    val value = Json.parseToJsonElement(content).jsonObject["presence"]?.jsonPrimitive?.content
    return value?.takeIf { it == "online" || it == "unavailable" || it == "offline" }
}
