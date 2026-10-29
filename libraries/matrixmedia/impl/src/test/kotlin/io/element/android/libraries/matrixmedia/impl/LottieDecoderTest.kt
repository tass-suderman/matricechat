/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrixmedia.impl

import android.animation.ValueAnimator
import androidx.test.platform.app.InstrumentationRegistry
import coil3.DrawableImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.ScaleDrawable
import com.airbnb.lottie.LottieDrawable
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.FileSystem
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

class LottieDecoderTest : RobolectricTest() {
    private val context get() = InstrumentationRegistry.getInstrumentation().context
    private val json = """{"v":"5.7.4","fr":30,"ip":0,"op":60,"w":128,"h":128,"layers":[]}"""

    @Test
    fun `gzipped and plain Lottie stickers decode with infinite looping even with wrong MIME type`() = runTest {
        for (data in listOf(gzip(json.toByteArray()), json.toByteArray())) {
            result(data).source.use { source ->
                val result = SourceFetchResult(source, "image/png", DataSource.MEMORY)
                val decoder = LottieDecoder.Factory().create(result, Options(context), ImageLoader(context))!!
                val image = decoder.decode()!!.image as DrawableImage
                val drawable = (image.drawable as ScaleDrawable).child as LottieDrawable
                // Robolectric changes infinite repeats to one; its shadow keeps the requested value.
                val animator = ReflectionHelpers.getField<ValueAnimator>(drawable, "animator")
                assertThat(shadowOf(animator).actualRepeatCount).isEqualTo(LottieDrawable.INFINITE)
                assertThat(drawable.composition!!.durationFrames).isGreaterThan(59f)
                assertThat(image.width).isGreaterThan(0)
            }
        }
    }

    @Test
    fun `ordinary images are left to the image decoder`() {
        val result = result(byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61))
        result.source.use {
            assertThat(LottieDecoder.Factory().create(result, Options(context), ImageLoader(context))).isNull()
        }
    }

    @Test
    fun `corrupt compressed sticker fails cleanly`() = runTest {
        val result = result(byteArrayOf(0x1f, 0x8b.toByte(), 0))
        result.source.use {
            try {
                LottieDecoder(result, Options(context)).decode()
                error("Expected corrupt gzip to fail")
            } catch (_: IOException) {
                // Coil displays its regular image error state.
            }
        }
    }

    @Test
    fun `JSON without a usable animation is rejected`() = runTest {
        val result = result(gzip("{}".toByteArray()))
        result.source.use {
            try {
                LottieDecoder(result, Options(context)).decode()
                error("Expected invalid animation to fail")
            } catch (error: IOException) {
                assertThat(error.message).isEqualTo("Invalid Lottie sticker")
            }
        }
    }

    @Test
    fun `oversized decompressed sticker is rejected`() = runTest {
        val result = result(gzip(ByteArray(4 * 1024 * 1024 + 1) { 32 }))
        result.source.use {
            try {
                LottieDecoder(result, Options(context)).decode()
                error("Expected oversized sticker to fail")
            } catch (error: IOException) {
                assertThat(error.message).isEqualTo("Lottie sticker is too large")
            }
        }
    }

    private fun result(data: ByteArray) = SourceFetchResult(
        ImageSource(Buffer().write(data), FileSystem.SYSTEM),
        null,
        DataSource.MEMORY,
    )

    private fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        GZIPOutputStream(output).use { it.write(data) }
    }.toByteArray()
}
