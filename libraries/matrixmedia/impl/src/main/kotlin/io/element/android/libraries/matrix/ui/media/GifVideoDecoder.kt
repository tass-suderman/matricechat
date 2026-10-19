/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.ui.media

import android.os.Build
import android.os.Process
import android.util.LruCache
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.Options
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.HashingSource
import okio.Path.Companion.toOkioPath
import okio.blackholeSink
import okio.buffer
import java.io.File
import java.io.IOException

/** Converts GIF-style MP4s and APNG stickers to animated GIFs supported by Android. */
internal class GifVideoDecoder(
    private val result: SourceFetchResult,
    private val options: Options,
    private val gifCache: LruCache<String, ByteArray>,
) : Decoder {
    override suspend fun decode(): DecodeResult = conversionMutex.withLock {
        withContext(Dispatchers.IO) {
            val format = if (isApng(result.source.source())) "apng" else "mp4"
            val input = result.source.file()
            val hashingSource = HashingSource.sha256(result.source.fileSystem.source(input))
            hashingSource.buffer().use { it.readAll(blackholeSink()) }
            val cacheKey = hashingSource.hash.hex()
            gifCache.get(cacheKey)?.let { bytes ->
                return@withContext decodeGif(ImageSource(Buffer().write(bytes), FileSystem.SYSTEM))
            }
            val output = File.createTempFile("gif-video-", ".gif", options.context.cacheDir)
            try {
                val args = arrayOf(
                    "-nostdin", "-v", "quiet", "-y", "-max_alloc", "67108864", "-threads", "1",
                    "-protocol_whitelist", "file", "-f", format, "-i", input.toString(),
                    // shortcut: preview at most 30 seconds at 480px/15fps; full video remains available on tap.
                    "-t", "30", "-filter_complex_threads", "1", "-filter_complex",
                    "fps=15,scale=w='min(480,iw)':h='min(480,ih)':force_original_aspect_ratio=decrease:flags=lanczos,split[a][b];" +
                        "[a]palettegen=stats_mode=single[p];[b][p]paletteuse=new=1",
                    "-an", "-loop", "0", "-fs", MAX_GIF_BYTES.toString(), output.absolutePath,
                )
                val completed = CompletableDeferred<FFmpegSession>()
                // Keep native logs out of logcat: they can contain media metadata.
                val session = FFmpegKit.executeWithArgumentsAsync(args, { completed.complete(it) }, {}, {})
                val finished = try {
                    withTimeout(60_000) { completed.await() }
                } finally {
                    if (!completed.isCompleted) {
                        FFmpegKit.cancel(session.sessionId)
                        // Do not delete files while native code is still using them.
                        withContext(NonCancellable) { completed.await() }
                    }
                }
                if (!ReturnCode.isSuccess(finished.returnCode) || output.length() !in 1..MAX_GIF_BYTES) {
                    throw IOException("Could not create animated video preview")
                }
                val decoded = decodeGif(ImageSource(output.toOkioPath(), FileSystem.SYSTEM))
                gifCache.put(cacheKey, output.readBytes())
                decoded
            } finally {
                output.delete()
            }
        }
    }

    private suspend fun decodeGif(source: ImageSource): DecodeResult = source.use {
        val decoder = if (Build.VERSION.SDK_INT >= 28) AnimatedImageDecoder(it, options) else GifDecoder(it, options)
        decoder.decode() ?: throw IOException("Could not decode animated video preview")
    }

    class Factory : Decoder.Factory {
        // Keep decrypted previews in memory only; this cache belongs to the session's image loader.
        private val gifCache = object : LruCache<String, ByteArray>(32 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = value.size
        }

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // The maintained FFmpeg package ships 64-bit ABIs; other devices keep normal video playback.
            if (!Process.is64Bit()) return null
            val source = result.source.source()
            return if (isMp4(source) || isApng(source)) GifVideoDecoder(result, options, gifCache) else null
        }
    }

    companion object {
        private val conversionMutex = Mutex()
        private const val MAX_GIF_BYTES = 20 * 1024 * 1024L
    }
}

internal fun isMp4(source: BufferedSource): Boolean = source.rangeEquals(4, "ftyp".encodeUtf8()) &&
    listOf("isom", "iso2", "mp41", "mp42", "avc1", "M4V ").any { source.rangeEquals(8, it.encodeUtf8()) }

internal fun isApng(source: BufferedSource): Boolean {
    if (!source.rangeEquals(0, "89504e470d0a1a0a".decodeHex())) return false
    source.peek().use { peek ->
        peek.skip(8)
        var scanned = 8L
        // Animation control must precede image data. Bound metadata scanning for malformed images.
        while (scanned < 64 * 1024) {
            if (!peek.request(8)) return false
            val length = peek.readInt().toLong() and 0xffffffffL
            val type = peek.readUtf8(4)
            if (type == "acTL") return length == 8L && peek.request(12)
            if (type == "IDAT" || type == "IEND" || length > 64 * 1024 - scanned - 12) return false
            if (!peek.request(length + 4)) return false
            peek.skip(length + 4)
            scanned += length + 12
        }
    }
    return false
}
