package io.element.android.libraries.emoji.impl.packs

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import org.junit.Test

class ImagePackParserTest {
    @Test
    fun `sticker image dimensions and MIME type survive pack parsing`() {
        val image = parseImagePack("""{"images":{"turtle":{"url":"mxc://example.org/turtle","info":{"w":320,"h":240,"mimetype":"image/webp"}}}}""").single()
        assertThat(image.info).isEqualTo("""{"w":320,"h":240,"mimetype":"image/webp"}""")
    }

    @Test
    fun `new room IDs without a server name remain available`() {
        assertThat(globalImagePackKeys("""{"rooms":{"!createEventHash":{"":{}}}}""").keys)
            .containsExactly(RoomId("!createEventHash"))
    }

    @Test
    fun `full room state only includes globally selected emote packs`() {
        val events = listOf(
            """{"type":"im.ponies.room_emotes","state_key":"global","content":{"images":{"wave":{"url":"mxc://example.org/wave"}}}}""",
            """{"type":"im.ponies.room_emotes","state_key":"local","content":{"images":{"hidden":{"url":"mxc://example.org/hidden"}}}}""",
            """{"type":"m.room.member","state_key":"global","content":{"images":{"invalid":{"url":"mxc://example.org/invalid"}}}}""",
        )
        assertThat(roomImagePacks(events, setOf("global")).map { it.shortcode }).containsExactly("wave")
    }

    @Test
    fun `only globally active state keys are selected across homeservers`() {
        val rooms =
            globalImagePackKeys("""{"rooms": {
            "!one:example.org": {"": {}, "discord-123": {}, "invalid": null},
            "!two:other.org": {"stickers": {}},
            "not-a-room": {"": {}},
            "!broken:example.org": []
        }}""")
        assertThat(rooms.keys).containsExactly(RoomId("!one:example.org"), RoomId("!two:other.org"))
        assertThat(rooms[RoomId("!one:example.org")]).containsExactly("", "discord-123")
        assertThat(rooms[RoomId("!two:other.org")]).containsExactly("stickers")
    }

    @Test
    fun `image usage overrides pack usage and absent usage supports both`() {
        val images =
            parseImagePack("""{
            "pack": {"usage": ["sticker"]},
            "images": {
                "wave": {"url": "mxc://example.org/wave", "body": "Waving", "usage": ["emoticon"]},
                "turtle": {"url": "mxc://example.org/turtle"}
            }
        }""")
        assertThat(images[0].usage).containsExactly("emoticon")
        assertThat(images[0].name).isEqualTo("Waving")
        assertThat(images[1].usage).containsExactly("sticker")
        assertThat(images[1].name).isEqualTo("turtle")
        assertThat(parseImagePack("""{"images":{"wave":{"url":"mxc://example.org/wave"}}}""").single().usage)
            .containsExactly("emoticon", "sticker")
    }

    @Test
    fun `room state event envelope is accepted`() {
        val images =
            parseImagePack("""{
            "type": "im.ponies.room_emotes", "state_key": "discord-123",
            "content": {"images":{"wave~123":{"url":"mxc://example.org/wave", "usage":["emoticon"]}}}
        }""")
        assertThat(images.single().shortcode).isEqualTo("wave~123")
    }

    @Test
    fun `malformed entries do not discard valid images and external URLs are rejected`() {
        val images =
            parseImagePack("""{"images":{
            "valid":{"url":"mxc://example.org/valid"},
            "external":{"url":"https://example.org/tracker"},
            "invalid":{"url":"mxc://example.org"},
            "number":{"url":42},
            "broken":null,
            "":{"url":"mxc://example.org/empty"}
        }}""")
        assertThat(images.map { it.shortcode }).containsExactly("valid")
    }

    @Test
    fun `missing and malformed packs are empty`() {
        for (raw in listOf(null, "{", "[]", "{}", "{\"images\":false}")) {
            assertThat(parseImagePack(raw)).isEmpty()
        }
    }
}
