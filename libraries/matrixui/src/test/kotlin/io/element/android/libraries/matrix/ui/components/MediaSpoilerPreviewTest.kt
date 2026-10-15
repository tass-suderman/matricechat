package io.element.android.libraries.matrix.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import coil3.size.Size

@org.robolectric.annotation.Config(sdk = [36])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class MediaSpoilerPreviewTest {
    @Test
    fun `blur removes fine details without changing the source bitmap`() = runTest {
        val input = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) input.setPixel(x, y, if ((x + y) % 2 == 0) Color.BLACK else Color.WHITE)
        }
        val blurred = SpoilerBlurTransformation.transform(input, Size.ORIGINAL)
        assertThat(blurred.width).isEqualTo(128)
        assertThat(Color.red(blurred.getPixel(64, 64))).isIn(120..135)
        assertThat(input.isRecycled).isFalse()
        assertThat(input.getPixel(0, 0)).isEqualTo(Color.BLACK)
    }
}
