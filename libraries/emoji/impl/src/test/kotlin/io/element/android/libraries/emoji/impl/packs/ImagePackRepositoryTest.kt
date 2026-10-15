package io.element.android.libraries.emoji.impl.packs

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.tests.testutils.testCoroutineDispatchers
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
}
