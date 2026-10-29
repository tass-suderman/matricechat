package chat.schildi.lib.preferences

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FriendProfile(val nickname: String = "", val color: Int? = null, val globalName: String? = null)

fun decodeFriendProfiles(value: String): Map<String, FriendProfile> =
    runCatching { Json.decodeFromString<Map<String, FriendProfile>>(value) }.getOrDefault(emptyMap())

fun encodeFriendProfiles(value: Map<String, FriendProfile>): String = Json.encodeToString(value)

fun FriendProfile.displayName(remoteName: String?, roomSpecific: Boolean, currentGlobalName: String? = globalName): String? = when {
    roomSpecific && !remoteName.isNullOrBlank() && remoteName != currentGlobalName -> remoteName
    nickname.isNotBlank() -> nickname
    else -> remoteName
}
