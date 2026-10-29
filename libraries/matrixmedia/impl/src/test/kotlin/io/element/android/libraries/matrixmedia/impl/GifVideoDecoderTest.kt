/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrixmedia.impl

import android.util.LruCache
import androidx.test.platform.app.InstrumentationRegistry
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Test
import org.robolectric.annotation.GraphicsMode
import java.io.File

class GifVideoDecoderTest : RobolectricTest() {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `cached GIF is reused without invoking native conversion`() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().context
        val input = File.createTempFile("gif-cache-test", ".mp4", context.cacheDir)
        val data = Buffer().writeInt(24).writeUtf8("ftypisom").readByteArray()
        input.writeBytes(data)
        val cache = LruCache<String, ByteArray>(1).apply {
            put(data.toByteString().sha256().hex(), "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7".decodeBase64()!!.toByteArray())
        }
        try {
            ImageSource(input.toOkioPath(), FileSystem.SYSTEM).use { source ->
                val result = SourceFetchResult(source, "video/mp4", DataSource.DISK)
                val decoded = GifVideoDecoder(result, Options(context), cache).decode()
                assertThat(decoded.image.width).isEqualTo(1)
                assertThat(decoded.image.height).isEqualTo(1)
            }
        } finally {
            input.delete()
        }
    }

    @Test
    fun `APNG control chunks are detected without consuming the original image`() {
        val source = Buffer().write("89504e470d0a1a0a".decodeHex())
            .writeInt(13).writeUtf8("IHDR").write(ByteArray(13)).writeInt(0)
            .writeInt(8).writeUtf8("acTL").writeInt(2).writeInt(0).writeInt(0)
        val size = source.size
        assertThat(isApng(source)).isTrue()
        assertThat(source.size).isEqualTo(size)
    }

    @Test
    fun `static PNG truncated and oversized metadata do not enter conversion`() {
        val png = "89504e470d0a1a0a".decodeHex()
        assertThat(isApng(Buffer().write(png).writeInt(3).writeUtf8("IDAT").writeInt(0))).isFalse()
        assertThat(isApng(Buffer().write(png).writeInt(8).writeUtf8("acTL"))).isFalse()
        assertThat(isApng(Buffer().write(png).writeInt(Int.MAX_VALUE).writeUtf8("tEXt"))).isFalse()
        assertThat(isApng(Buffer().writeUtf8("GIF89a"))).isFalse()
    }

    @Test
    fun `MP4 signatures are accepted without trusting MIME types`() {
        for (brand in listOf("isom", "iso2", "mp41", "mp42", "avc1", "M4V ")) {
            assertThat(isMp4(Buffer().writeInt(24).writeUtf8("ftyp$brand"))).isTrue()
        }
    }

    @Test
    fun `HEIF AVIF images ordinary images and truncated files are not videos`() {
        for (brand in listOf("heic", "heix", "mif1", "avif")) {
            assertThat(isMp4(Buffer().writeInt(24).writeUtf8("ftyp$brand"))).isFalse()
        }
        assertThat(isMp4(Buffer().writeUtf8("GIF89a"))).isFalse()
        assertThat(isMp4(Buffer().writeInt(24).writeUtf8("ftyp"))).isFalse()
        assertThat(isMp4(Buffer())).isFalse()
    }
}
