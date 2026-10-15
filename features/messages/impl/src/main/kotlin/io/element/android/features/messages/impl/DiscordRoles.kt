package io.element.android.features.messages.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.BaseRoom
import io.element.android.libraries.matrix.ui.messages.sender.DiscordRole
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@Composable
internal fun discordRoles(client: MatrixClient, room: BaseRoom, dispatchers: CoroutineDispatchers): Map<String, DiscordRole> {
    val roles by produceState<Map<String, DiscordRole>>(emptyMap(), client, room) {
        value = emptyMap()
        val rolesByRoom = mutableMapOf<RoomId, Map<String, DiscordRole>>()
        coroutineScope {
            val visited = mutableSetOf<RoomId>()
            val pending = ArrayDeque<RoomId>().apply { add(room.roomId) }
            while (pending.isNotEmpty()) {
                val id = pending.removeFirst()
                if (!visited.add(id)) continue
                pending.addAll(client.spaceService.joinedParents(id).getOrNull().orEmpty().map { it.roomId })
                val parent = client.getJoinedRoom(id) ?: continue
                launch {
                    parent.use {
                        do {
                            val state = parent.fetchFullRoomState().getOrNull()
                            if (state != null) {
                                val (updated, isDiscord) = withContext(dispatchers.computation) {
                                    parseDiscordRoles(state) to hasDiscordMetadata(state)
                                }
                                rolesByRoom[id] = updated
                                value = rolesByRoom.values.fold(emptyMap()) { all, roles -> all + roles }
                                if (!isDiscord) return@use
                            }
                            // shortcut: the SDK cannot fetch custom state types from the server individually; refresh until it can.
                            delay(30_000)
                        } while (isActive)
                    }
                }
            }
        }
    }
    return roles
}

internal fun parseDiscordRoles(state: List<String>): Map<String, DiscordRole> {
    val events = state.mapNotNull { raw ->
        runCatchingExceptions { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
    }
    val enabled = events.any {
        it.text("type") == "fi.mau.discord.role_tracking" && it.text("state_key") == "" &&
            ((it["content"] as? JsonObject)?.get("enabled") as? JsonPrimitive)?.booleanOrNull == true
    }
    if (!enabled) return emptyMap()
    return events.mapNotNull { event ->
        if (event.text("type") != "fi.mau.discord.member_role") return@mapNotNull null
        val userId = event.text("state_key")?.takeIf { it.startsWith("@") } ?: return@mapNotNull null
        val content = event["content"] as? JsonObject ?: return@mapNotNull null
        val name = content.text("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val color = content.text("color")?.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: return@mapNotNull null
        userId to DiscordRole(name, 0xff000000L or color.drop(1).toLong(16))
    }.toMap()
}

private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

/** Resolve per-message Discord identity for double puppets without changing the displayed Matrix sender. */
internal fun discordRoleUserId(raw: String?, roles: Map<String, DiscordRole>): String? {
    val event = runCatchingExceptions { raw?.let { Json.parseToJsonElement(it) as? JsonObject } }.getOrNull()
    val content = event?.get("content") as? JsonObject ?: return null
    val profile = content["com.beeper.per_message_profile"] as? JsonObject ?: return null
    val id = profile.text("id")?.takeIf { it.matches(Regex("[0-9]+_[0-9]+")) }?.substringAfter('_') ?: return null
    val remoteId = Regex("(?<![0-9])$id(?![0-9])")
    return roles.keys.singleOrNull { remoteId.containsMatchIn(it.substringBefore(':')) }
}

private fun hasDiscordMetadata(state: List<String>): Boolean = state.any { raw ->
    val event = runCatchingExceptions { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
    val content = event?.get("content") as? JsonObject
    val protocol = content?.get("protocol") as? JsonObject
    event?.text("type") == "fi.mau.discord.role_tracking" ||
        (event?.text("type") == "m.bridge" && protocol?.text("id") == "discordgo")
}
