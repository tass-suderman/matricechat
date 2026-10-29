package io.element.android.libraries.matrix.ui.media

import android.graphics.drawable.AnimatedImageDrawable
import androidx.test.platform.app.InstrumentationRegistry
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.media.FakeMatrixMediaLoader
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

class DefaultImageLoaderFactoryTest : RobolectricTest() {
    @Test
    fun `Matrix loader retains the custom animation decoders and Matrix fetchers`() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val loader = DefaultImageLoaderFactory(context, ::OkHttpClient).newImageLoader(FakeMatrixMediaLoader())
        try {
            assertThat(loader.components.decoderFactories.any { it is GifVideoDecoder.Factory }).isTrue()
            assertThat(loader.components.decoderFactories.any { it is LottieDecoder.Factory }).isTrue()
            assertThat(loader.components.fetcherFactories.any { it.first is MediaRequestDataFetcherFactory }).isTrue()
        } finally {
            loader.shutdown()
        }
    }

    @Test
    fun `Matrix loader selects APNG conversion for animated PNG bytes`() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val loader = DefaultImageLoaderFactory(context, ::OkHttpClient).newImageLoader(FakeMatrixMediaLoader())
        val was64Bit = android.os.Process.is64Bit()
        org.robolectric.shadows.ShadowVMRuntime.setIs64Bit(true)
        try {
            val bytes = requireNotNull(javaClass.getResourceAsStream("/animated-sticker.png")).use { it.readBytes() }
            coil3.decode.ImageSource(okio.Buffer().write(bytes), okio.FileSystem.SYSTEM).use { source ->
                val result = coil3.fetch.SourceFetchResult(source, "image/png", coil3.decode.DataSource.MEMORY)
                val decoder = loader.components.newDecoder(result, coil3.request.Options(context), loader)
                assertThat(decoder?.first).isInstanceOf(GifVideoDecoder::class.java)
            }
        } finally {
            org.robolectric.shadows.ShadowVMRuntime.setIs64Bit(was64Bit)
            loader.shutdown()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `Matrix loader decodes GIF bytes as an animated drawable`() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().context
        val loader = DefaultImageLoaderFactory(context, ::OkHttpClient).newImageLoader(FakeMatrixMediaLoader())
        try {
            val bytes = requireNotNull(javaClass.getResourceAsStream("/animated-sticker.gif")).use { it.readBytes() }
            val result = loader.execute(ImageRequest.Builder(context).data(java.nio.ByteBuffer.wrap(bytes)).build())
            assertThat(result).isInstanceOf(SuccessResult::class.java)
            val drawable = (result as SuccessResult).image.asDrawable(context.resources)
            val original = (drawable as? coil3.size.ScaleDrawable)?.child ?: drawable
            assertThat(original).isInstanceOf(AnimatedImageDrawable::class.java)
        } finally {
            loader.shutdown()
        }
    }
}
