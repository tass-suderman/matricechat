/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrixmedia.impl

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.ScaleDrawable
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.decodeHex
import okio.buffer
import okio.source
import java.io.IOException
import java.util.zip.GZIPInputStream

/** Telegram/Discord TGS stickers are gzipped Lottie JSON, regardless of the reported MIME type. */
internal class LottieDecoder(private val result: SourceFetchResult, private val options: Options) : Decoder {
    override suspend fun decode(): DecodeResult = withContext(Dispatchers.IO) {
        val json = result.source.source().inputStream().use { input ->
            val stream = if (result.source.source().rangeEquals(0, GZIP_MAGIC)) GZIPInputStream(input) else input
            stream.source().buffer().use { source ->
                source.request(MAX_JSON_BYTES + 1)
                val bytes = source.buffer.readByteArray()
                if (bytes.size > MAX_JSON_BYTES) throw IOException("Lottie sticker is too large")
                bytes
            }
        }
        val parsed = LottieCompositionFactory.fromJsonInputStreamSync(json.inputStream(), null)
        val composition = parsed.value ?: throw IOException("Invalid Lottie sticker")
        if (composition.bounds.isEmpty || !composition.durationFrames.isFinite() || composition.durationFrames <= 0 ||
            !composition.frameRate.isFinite() || composition.frameRate <= 0
        ) {
            throw IOException("Invalid Lottie sticker")
        }
        val drawable = LottieDrawable().apply {
            setComposition(composition)
            repeatCount = LottieDrawable.INFINITE
        }
        DecodeResult(ScaleDrawable(drawable, options.scale).asImage(), isSampled = false)
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val source = result.source.source()
            val isLottie = source.rangeEquals(0, GZIP_MAGIC) ||
                result.mimeType == "application/json" || source.rangeEquals(0, JSON_MAGIC)
            return if (isLottie) LottieDecoder(result, options) else null
        }
    }

    companion object {
        private val GZIP_MAGIC = "1f8b".decodeHex()
        private val JSON_MAGIC = "7b".decodeHex()
        private const val MAX_JSON_BYTES = 4 * 1024 * 1024L
    }
}
