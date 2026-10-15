package io.element.android.libraries.matrix.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.Transformation
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.IconButton

/** Blur the decoded bitmap so spoilers stay concealed on Android versions before API 31 too. */
internal object SpoilerBlurTransformation : Transformation() {
    override val cacheKey = "tortue-spoiler-blur-v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val longestSide = maxOf(input.width, input.height)
        val width = maxOf(1, input.width * 128 / longestSide)
        val height = maxOf(1, input.height * 128 / longestSide)
        val tiny = Bitmap.createScaledBitmap(input, maxOf(1, width / 16), maxOf(1, height / 16), true)
        return Bitmap.createScaledBitmap(tiny, width, height, true)
    }
}

@Composable
fun MediaSpoilerPreview(
    model: Any?,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val revealLabel = stringResource(chat.schildi.lib.R.string.action_reveal_media_spoiler)
    Box(modifier.clickable(onClickLabel = revealLabel, onClick = onReveal), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .allowHardware(false)
                .transformations(SpoilerBlurTransformation)
                .build(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        IconButton(
            onClick = onReveal,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
            ),
        ) {
            Icon(imageVector = CompoundIcons.VisibilityOn(), contentDescription = revealLabel)
        }
    }
}

@io.element.android.libraries.designsystem.preview.PreviewsDayNight
@Composable
internal fun MediaSpoilerPreviewPreview() = io.element.android.libraries.designsystem.preview.ElementPreview {
    MediaSpoilerPreview(
        model = io.element.android.libraries.designsystem.R.drawable.sample_background,
        onReveal = {},
        modifier = Modifier.fillMaxSize(),
    )
}
