package io.element.android.features.messages.impl.messagecomposer

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.matrix.test.AN_EVENT_ID
import io.element.android.libraries.matrix.test.A_THREAD_ID
import kotlinx.serialization.json.Json
import org.junit.Test

class PickerMessagesTest {
    private val sticker = CustomImage("turtle", "A turtle", "mxc://example.org/turtle", setOf("sticker"), "{\"w\":320,\"h\":240,\"mimetype\":\"image/webp\"}")

    @Test
    fun `sticker sends the pack image metadata and selected reply`() {
        val result = Json.parseToJsonElement(stickerContent(sticker, AN_EVENT_ID, null))
        assertThat(result).isEqualTo(Json.parseToJsonElement("""{
            "body":"A turtle","url":"mxc://example.org/turtle","info":{"w":320,"h":240,"mimetype":"image/webp"},
            "m.relates_to":{"m.in_reply_to":{"event_id":"${AN_EVENT_ID.value}"}}
        }"""))
    }

    @Test
    fun `thread sticker keeps explicit reply and thread root`() {
        val result = stickerContent(sticker, AN_EVENT_ID, A_THREAD_ID)
        assertThat(result).contains("\"rel_type\":\"m.thread\"")
        assertThat(result).contains("\"event_id\":\"${A_THREAD_ID.value}\"")
        assertThat(result).contains("\"is_falling_back\":false")
        assertThat(result).contains("\"m.in_reply_to\":{\"event_id\":\"${AN_EVENT_ID.value}\"}")
    }

    @Test
    fun `thread sticker without explicit reply falls back to thread root`() {
        val result = stickerContent(sticker.copy(info = null), null, A_THREAD_ID)
        assertThat(result).contains("\"info\":{}")
        assertThat(result).contains("\"is_falling_back\":true")
        assertThat(result).contains("\"m.in_reply_to\":{\"event_id\":\"${A_THREAD_ID.value}\"}")
    }
}
