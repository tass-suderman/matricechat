/*
 * Copyright (c) 2026 Element Creations Ltd.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

package io.element.android.libraries.matrix.impl.timeline

import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.ThreadId
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal fun replyPingExtraContent(eventId: EventId, ping: Boolean, threadRootId: ThreadId?): String = buildJsonObject {
    put("fi.mau.discord.reply_ping", ping)
    putJsonObject("m.relates_to") {
        if (threadRootId != null) {
            put("rel_type", "m.thread")
            put("event_id", threadRootId.value)
            put("is_falling_back", false)
        }
        putJsonObject("m.in_reply_to") { put("event_id", eventId.value) }
    }
}.toString()
