/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import com.beeper.android.messageformat.MatrixBodyParseResult
import io.element.android.libraries.matrix.api.media.MediaSource
import java.net.URI
import kotlin.time.Duration

data class TimelineItemVideoContent(
    override val filename: String,
    override val fileSize: Long?,
    override val caption: String?,
    override val formattedCaption: CharSequence?,
    override val formattedCaptionSc: MatrixBodyParseResult?,
    override val htmlCaption: String? = null,
    override val isEdited: Boolean,
    val duration: Duration,
    override val mediaSource: MediaSource,
    val thumbnailSource: MediaSource?,
    val aspectRatio: Float?,
    val blurHash: String?,
    val height: Int?,
    val width: Int?,
    val thumbnailWidth: Int?,
    val thumbnailHeight: Int?,
    override val mimeType: String,
    override val formattedFileSize: String,
    override val fileExtension: String,
) : TimelineItemEventContentWithAttachment {
    override val type: String = "TimelineItemImageContent"

    val showCaption = caption != null
}

/** Bridges often keep the provider page URL as the filename/caption and upload the video to MXC. */
internal val TimelineItemVideoContent.isGifVideo: Boolean
    get() = (mimeType.equals("video/mp4", ignoreCase = true) || fileExtension.equals("mp4", ignoreCase = true)) &&
        listOfNotNull(filename, caption, mediaSource.safeUrl).any(::isGifProviderUrl)

private fun isGifProviderUrl(value: String): Boolean {
    var uri = runCatching { URI(value) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() !in listOf("https", "http")) return false
    val host = uri.host?.lowercase() ?: return false
    if (host.startsWith("images-ext-") && host.endsWith(".discordapp.net")) {
        val nested = uri.rawPath.substringAfter("/https/", "")
        if (nested.isEmpty()) return false
        uri = runCatching { URI("https://$nested") }.getOrNull() ?: return false
    }
    val providerHost = uri.host?.lowercase() ?: return false
    return listOf("klipy.com", "tenor.com").any { providerHost == it || providerHost.endsWith(".$it") }
}
