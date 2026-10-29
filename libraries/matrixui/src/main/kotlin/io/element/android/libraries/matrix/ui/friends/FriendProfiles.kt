package io.element.android.libraries.matrix.ui.friends

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import chat.schildi.lib.preferences.FriendProfile
import chat.schildi.lib.preferences.displayName
import io.element.android.libraries.matrix.api.room.RoomInfo

/** Presentation-only overrides; never feed these names into outgoing messages or server profiles. */
data class FriendProfiles(
    val profiles: Map<String, FriendProfile> = emptyMap(),
    val globalNames: Map<String, String?> = emptyMap(),
    val save: (suspend (String, FriendProfile) -> Unit)? = null,
) {
    fun name(userId: String, remoteName: String?, roomSpecific: Boolean = false): String? {
        val profile = profiles[userId] ?: return remoteName
        val global = if (userId in globalNames) globalNames[userId] else profile.globalName
        return profile.displayName(remoteName, roomSpecific, global)
    }

    fun nameColor(userId: String, roomColor: Long? = null): Color? =
        roomColor?.let(::Color) ?: profiles[userId]?.color?.let(::Color)

    fun roomName(room: RoomInfo): String? {
        if (!room.isDm || !room.rawName.isNullOrBlank() || !room.privateRoomName.isNullOrBlank()) return room.privateRoomName ?: room.name
        val hero = room.heroes.singleOrNull() ?: return room.name
        return name(hero.userId.value, hero.displayName, roomSpecific = true) ?: room.name
    }
}

val LocalFriendProfiles = staticCompositionLocalOf { FriendProfiles() }
