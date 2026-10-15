package io.element.android.libraries.emoji.impl.packs

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomType
import io.element.android.libraries.matrix.api.spaces.SpaceRoom
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.spaces.FakeSpaceService
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

class ImagePackRepositoryTest {
    private val globalRoomId = RoomId("!global:example.org")
    private val globalReferences = """{"rooms":{"!global:example.org":{"enabled":{}}}}"""
    private val globalEvent = """{"type":"im.ponies.room_emotes","state_key":"enabled","content":{"images":{"global":{"url":"mxc://example.org/global"}}}}"""

    @Test
    fun `personal and globally selected packs load without cached room state`() = runTest {
        val personal = """{"images":{"personal":{"url":"mxc://example.org/personal"}}}"""
        val client = FakeMatrixClient(getAccountDataLambda = { type ->
            Result.success(if (type == "im.ponies.user_emotes") personal else globalReferences)
        })
        val room = FakeBaseRoom(fetchFullRoomStateResult = {
            Result.success(listOf(globalEvent, globalEvent.replace("enabled", "unselected").replace("global", "hidden")))
        })
        client.givenGetRoomResult(globalRoomId, room)
        val images = DefaultImagePackRepository(client, testCoroutineDispatchers()).getImages().getOrThrow()
        assertThat(images.map { it.shortcode }).containsExactly("personal", "global")
        room.assertDestroyed()
    }

    @Test
    fun `cached packs remain usable when the state refresh fails`() = runTest {
        val client = FakeMatrixClient(getAccountDataLambda = { type ->
            Result.success(if (type == "im.ponies.user_emotes") null else globalReferences)
        })
        val room = FakeBaseRoom(
            fetchFullRoomStateResult = { Result.failure(IOException("offline")) },
            getRawStateResult = { type, key ->
                assertThat(type).isEqualTo("im.ponies.room_emotes")
                assertThat(key).isEqualTo("enabled")
                Result.success(globalEvent)
            },
        )
        client.givenGetRoomResult(globalRoomId, room)
        assertThat(DefaultImagePackRepository(client, testCoroutineDispatchers()).getImages().getOrThrow().single().shortcode).isEqualTo("global")
        room.assertDestroyed()
    }
    @Test
    fun `Discord room emoji and stickers precede personal and globally enabled images`() = runTest {
        val roomId = RoomId("!discord:example.org")
        val local = """
            {
                "type": "im.ponies.room_emotes",
                "state_key": "discord-123",
                "content": {
                    "fi.mau.discord.guild_id": "123",
                    "images": {
                        "wave~456": {
                            "url": "mxc://example.org/wave",
                            "usage": [
                                "emoticon"
                            ]
                        },
                        "sticker~789": {
                            "url": "mxc://example.org/sticker",
                            "usage": [
                                "sticker"
                            ]
                        }
                    }
                }
            }
        """.trimIndent()
        val client = FakeMatrixClient(
            spaceService = FakeSpaceService(joinedParentsResult = { Result.success(emptyList()) }),
            getAccountDataLambda = { type ->
                Result.success(if (type == "im.ponies.user_emotes") """{"images":{"personal":{"url":"mxc://example.org/personal"}}}""" else globalReferences)
            },
        )
        val room = FakeBaseRoom(fetchFullRoomStateResult = { Result.success(listOf(local)) })
        client.givenGetRoomResult(roomId, room)
        client.givenGetRoomResult(globalRoomId, FakeBaseRoom(fetchFullRoomStateResult = { Result.success(listOf(globalEvent)) }))
        val images = DefaultImagePackRepository(client, testCoroutineDispatchers()).getImagesForRoom(roomId).getOrThrow()
        assertThat(images.map { it.shortcode }).containsExactly("wave~456", "sticker~789", "personal", "global").inOrder()
        assertThat(images[0].usage).containsExactly("emoticon")
        assertThat(images[1].usage).containsExactly("sticker")
        room.assertDestroyed()
    }

    @Test
    fun `parent space pack loads without globally enabling it and cycles are ignored`() = runTest {
        val roomId = RoomId("!channel:example.org")
        val space = SpaceRoom(
            rawName = null,
            displayName = "Discord",
            avatarUrl = null,
            canonicalAlias = null,
            childrenCount = 1,
            guestCanJoin = false,
            heroes = persistentListOf(),
            joinRule = null,
            numJoinedMembers = 1,
            roomId = globalRoomId,
            roomType = RoomType.Space,
            state = null,
            topic = null,
            worldReadable = false,
            via = persistentListOf(),
            isDirect = false,
            isDm = false,
        )
        val client = FakeMatrixClient(
            spaceService = FakeSpaceService(joinedParentsResult = { Result.success(listOf(space)) }),
            getAccountDataLambda = { Result.success(null) },
        )
        client.givenGetRoomResult(roomId, FakeBaseRoom(fetchFullRoomStateResult = { Result.success(emptyList()) }))
        client.givenGetRoomResult(globalRoomId, FakeBaseRoom(fetchFullRoomStateResult = { Result.success(listOf(globalEvent)) }))
        val images = DefaultImagePackRepository(client, testCoroutineDispatchers()).getImagesForRoom(roomId).getOrThrow()
        assertThat(images.map { it.shortcode }).containsExactly("global")
    }
}
