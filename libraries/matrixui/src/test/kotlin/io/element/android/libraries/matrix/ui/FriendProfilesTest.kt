package io.element.android.libraries.matrix.ui

import chat.schildi.lib.preferences.FriendProfile
import chat.schildi.lib.preferences.decodeFriendProfiles
import chat.schildi.lib.preferences.encodeFriendProfiles
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.libraries.matrix.ui.friends.FriendProfiles
import org.junit.Test

class FriendProfilesTest {
    private val id = "@friend:example.org"
    private val profile = FriendProfile("Best friend", 0xff123456.toInt(), "Public name")
    private val friends = FriendProfiles(mapOf(id to profile))

    @Test
    fun `friend name replaces public name but room nickname wins`() {
        assertThat(friends.name(id, "Public name", roomSpecific = true)).isEqualTo("Best friend")
        assertThat(friends.name(id, "Discord nickname", roomSpecific = true)).isEqualTo("Discord nickname")
        assertThat(friends.name(id, "Public name")).isEqualTo("Best friend")
        assertThat(friends.name(id, null)).isEqualTo("Best friend")
        assertThat(friends.name("@other:example.org", "Other")).isEqualTo("Other")
    }

    @Test
    fun `fresh global name distinguishes public profile updates from room nicknames`() {
        val updated = friends.copy(globalNames = mapOf(id to "New public name"))
        assertThat(updated.name(id, "New public name", roomSpecific = true)).isEqualTo("Best friend")
        assertThat(updated.name(id, "Discord nickname", roomSpecific = true)).isEqualTo("Discord nickname")
    }

    @Test
    fun `friend settings round trip including unicode nickname and alpha colour`() {
        val entries = mapOf(id to profile.copy(nickname = "Amie 🐢", color = 0x80112233.toInt()))
        assertThat(decodeFriendProfiles(encodeFriendProfiles(entries))).isEqualTo(entries)
        assertThat(decodeFriendProfiles("invalid")).isEmpty()
    }

    @Test
    fun `only generated one to one room names use friend nickname`() {
        val room = aRoomInfo(name = "Public name", rawName = null, isDm = true, heroes = listOf(MatrixUser(UserId(id), "Public name")))
        assertThat(friends.roomName(room)).isEqualTo("Best friend")
        assertThat(friends.roomName(room.copy(rawName = "Custom room", name = "Custom room"))).isEqualTo("Custom room")
        assertThat(friends.roomName(room.copy(privateRoomName = "Private room"))).isEqualTo("Private room")
        assertThat(friends.roomName(room.copy(isDm = false))).isEqualTo("Public name")
        assertThat(friends.roomName(room.copy(heroes = kotlinx.collections.immutable.persistentListOf()))).isEqualTo("Public name")
        assertThat(room.heroes.single().displayName).isEqualTo("Public name")
    }

    @Test
    fun `room role colour takes precedence over the private friend colour`() {
        assertThat(friends.nameColor(id)).isEqualTo(androidx.compose.ui.graphics.Color(0xff123456.toInt()))
        assertThat(friends.nameColor(id, 0xffee2200L)).isEqualTo(androidx.compose.ui.graphics.Color(0xffee2200L))
        assertThat(friends.nameColor("@other:example.org")).isNull()
    }

    @Test
    fun `colour only does not replace public or room names`() {
        val colours = FriendProfiles(mapOf(id to profile.copy(nickname = "")))
        assertThat(colours.name(id, "Public name")).isEqualTo("Public name")
        assertThat(colours.name(id, "Room name", roomSpecific = true)).isEqualTo("Room name")
    }
}
