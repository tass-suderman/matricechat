/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */
package io.element.android.libraries.matrix.impl.timeline

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.ThreadId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ReplyPingExtraContentTest {
    @Test
    fun `quiet reply preserves its target and sends a boolean false`() {
        val content = Json.parseToJsonElement(replyPingExtraContent(EventId("\$reply"), false, null)).jsonObject
        assertThat(content.getValue("fi.mau.discord.reply_ping").jsonPrimitive.boolean).isFalse()
        val relation = content.getValue("m.relates_to").jsonObject
        assertThat(relation.getValue("m.in_reply_to").jsonObject.getValue("event_id").jsonPrimitive.content).isEqualTo("\$reply")
        assertThat(relation.containsKey("rel_type")).isFalse()
    }

    @Test
    fun `pinging thread reply preserves root and explicit reply target`() {
        val content = Json.parseToJsonElement(replyPingExtraContent(EventId("\$reply"), true, ThreadId("\$root"))).jsonObject
        assertThat(content.getValue("fi.mau.discord.reply_ping").jsonPrimitive.boolean).isTrue()
        val relation = content.getValue("m.relates_to").jsonObject
        assertThat(relation.getValue("rel_type").jsonPrimitive.content).isEqualTo("m.thread")
        assertThat(relation.getValue("event_id").jsonPrimitive.content).isEqualTo("\$root")
        assertThat(relation.getValue("is_falling_back").jsonPrimitive.boolean).isFalse()
        assertThat(relation.getValue("m.in_reply_to").jsonObject.getValue("event_id").jsonPrimitive.content).isEqualTo("\$reply")
    }
}
