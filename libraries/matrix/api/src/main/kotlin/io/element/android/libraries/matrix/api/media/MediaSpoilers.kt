package io.element.android.libraries.matrix.api.media

import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.timeline.item.event.GalleryItemType
import io.element.android.libraries.matrix.api.timeline.item.event.GalleryMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.ImageMessageType
import io.element.android.libraries.matrix.api.timeline.item.event.MessageType
import io.element.android.libraries.matrix.api.timeline.item.event.VideoMessageType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

const val MEDIA_SPOILER_KEY = "page.codeberg.everypizza.msc4193.spoiler"
const val MEDIA_SPOILER_CONTENT = "{\"page.codeberg.everypizza.msc4193.spoiler\":true}"

fun mediaSpoilerFlag(raw: String?): Boolean? = raw?.let {
    runCatchingExceptions {
        val event = Json.parseToJsonElement(it) as? JsonObject
        val content = event?.get("content") as? JsonObject ?: event
        val replacement = content?.get("m.new_content") as? JsonObject ?: content
        (replacement?.get(MEDIA_SPOILER_KEY) as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
    }.getOrNull()
}

fun MessageType.withMediaSpoiler(spoiler: Boolean): MessageType = when (this) {
    is ImageMessageType -> copy(source = source.copy(isSpoiler = spoiler), info = info?.copy(
        thumbnailSource = info.thumbnailSource?.copy(isSpoiler = spoiler),
    ))
    is VideoMessageType -> copy(source = source.copy(isSpoiler = spoiler), info = info?.copy(
        thumbnailSource = info.thumbnailSource?.copy(isSpoiler = spoiler),
    ))
    is GalleryMessageType -> copy(items = items.map { item ->
        when (item) {
            is GalleryItemType.Image -> item.copy(content = item.content.withMediaSpoiler(spoiler) as ImageMessageType)
            is GalleryItemType.Video -> item.copy(content = item.content.withMediaSpoiler(spoiler) as VideoMessageType)
            else -> item
        }
    })
    else -> this
}
