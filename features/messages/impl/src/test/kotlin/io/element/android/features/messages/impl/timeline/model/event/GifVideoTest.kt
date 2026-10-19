/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.media.MediaSource
import org.junit.Test

class GifVideoTest {
    @Test
    fun `provider MP4s include direct media and bridged page URLs`() {
        listOf(
            "https://media.tenor.com/RAqYzw1bfSAAAAPo/greta-thunberg.mp4",
            "https://static.klipy.com/ii/example.mp4?token=x",
            "https://tenor.com/view/example-123",
            "https://klipy.com/gifs/example",
            "https://images-ext-1.discordapp.net/external/hash/https/static.klipy.com/ii/example.mp4",
        ).forEach { url ->
            assertThat(aTimelineItemVideoContent().copy(filename = url).isGifVideo).isTrue()
        }
    }

    @Test
    fun `provider URL may be in caption or unencrypted source`() {
        val url = "https://media.tenor.com/example.mp4"
        assertThat(aTimelineItemVideoContent(caption = url).isGifVideo).isTrue()
        assertThat(aTimelineItemVideoContent(mediaSource = MediaSource(url)).isGifVideo).isTrue()
    }

    @Test
    fun `ordinary videos and lookalike hosts stay videos`() {
        listOf(
            "Video.mp4", "https://example.com/klipy.com/example.mp4", "https://notklipy.com/example.mp4",
            "https://tenor.com.evil.example/example.mp4", "https://tenor.com@evil.example/example.mp4",
            "https://images-ext-1.discordapp.net/invalid/example.mp4", "https://[invalid", "file://tenor.com/example.mp4",
        ).forEach { url ->
            assertThat(aTimelineItemVideoContent().copy(filename = url).isGifVideo).isFalse()
        }
        assertThat(aTimelineItemVideoContent().copy(filename = "https://tenor.com/example", mimeType = "video/webm", fileExtension = "webm").isGifVideo)
            .isFalse()
    }
}
