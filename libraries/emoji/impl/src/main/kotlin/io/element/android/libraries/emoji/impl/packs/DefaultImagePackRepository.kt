package io.element.android.libraries.emoji.impl.packs

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.emoji.api.packs.ImagePackRepository
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val mxcUrl = Regex("""mxc://[^/\s?#<>]+/[^/\s?#<>]+""")

@Inject
@ContributesBinding(SessionScope::class)
class DefaultImagePackRepository(private val client: MatrixClient, private val dispatchers: CoroutineDispatchers) : ImagePackRepository {
    override suspend fun getImages(): Result<List<CustomImage>> = getImagesForRoom(null)

    override suspend fun getImagesForRoom(roomId: RoomId?): Result<List<CustomImage>> = withContext(dispatchers.computation) {
        runCatchingExceptions {
            val images = mutableListOf<CustomImage>()
            if (roomId != null) {
                val visited = mutableSetOf<RoomId>()
                val pending = ArrayDeque<RoomId>().apply { add(roomId) }
                while (pending.isNotEmpty()) {
                    val id = pending.removeFirst()
                    if (!visited.add(id)) continue
                    pending.addAll(client.spaceService.joinedParents(id).getOrNull().orEmpty().map { it.roomId })
                    client.getRoom(id)?.use { room ->
                        val state = room.fetchFullRoomState().getOrNull()
                            ?: room.getRawState("im.ponies.room_emotes").getOrNull().orEmpty()
                        images += roomImagePacks(state, null)
                    }
                }
            }
            images += parseImagePack(client.getAccountData("im.ponies.user_emotes").getOrThrow())
            val rooms = globalImagePackKeys(client.getAccountData("im.ponies.emote_rooms").getOrThrow())
            rooms.forEach { (roomId, keys) ->
                client.getRoom(roomId)?.use { room ->
                    // shortcut: the SDK only exposes a whole-room state fetch; use a per-event fetch when it becomes available.
                    val state = room.fetchFullRoomState().getOrNull()
                    images += if (state != null) {
                        roomImagePacks(state, keys)
                    } else {
                        keys.flatMap { stateKey -> parseImagePack(room.getRawState("im.ponies.room_emotes", stateKey).getOrNull()) }
                    }
                }
            }
            images.distinctBy { Triple(it.url, it.shortcode, it.usage) }
        }
    }
}

internal fun globalImagePackKeys(raw: String?): Map<RoomId, Set<String>> {
    val rooms = imagePackContent(raw)?.get("rooms") as? JsonObject ?: return emptyMap()
    return rooms.mapNotNull { (roomId, value) ->
        if (!roomId.startsWith("!") || roomId.length < 2 || roomId.any { it.isWhitespace() } || value !is JsonObject) return@mapNotNull null
        RoomId(roomId) to value.filterValues { it is JsonObject }.keys
    }.toMap()
}

private fun jsonObject(raw: String?): JsonObject? = raw?.let {
    runCatchingExceptions { Json.parseToJsonElement(it) as? JsonObject }.getOrNull()
}

internal fun imagePackContent(raw: String?): JsonObject? = jsonObject(raw)?.let {
    it["content"] as? JsonObject ?: it
}

internal fun roomImagePacks(state: List<String>, keys: Set<String>?): List<CustomImage> = state.flatMap { raw ->
    val event = jsonObject(raw)
    if (event?.text("type") == "im.ponies.room_emotes" && (keys == null || event.text("state_key") in keys)) {
        parseImagePack(raw)
    } else {
        emptyList()
    }
}

internal fun parseImagePack(raw: String?): List<CustomImage> {
    val content = imagePackContent(raw) ?: return emptyList()
    val pack = content["pack"] as? JsonObject
    val packUsage = pack?.usage() ?: setOf("emoticon", "sticker")
    val images = content["images"] as? JsonObject ?: return emptyList()
    return images.mapNotNull { (shortcode, value) ->
        val image = value as? JsonObject ?: return@mapNotNull null
        val url = image.text("url") ?: return@mapNotNull null
        if (shortcode.isBlank() || !url.matches(mxcUrl)) return@mapNotNull null
        CustomImage(
            shortcode = shortcode,
            name = image.text("body")?.takeIf { it.isNotBlank() } ?: shortcode,
            url = url,
            usage = image.usage() ?: packUsage,
            info = (image["info"] as? JsonObject)?.toString(),
        )
    }
}

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.usage(): Set<String>? = (get("usage") as? JsonArray)?.mapNotNull {
    (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content
}?.toSet()
