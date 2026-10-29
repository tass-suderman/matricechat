package io.element.android.libraries.emoji.impl.picker

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.CircularProgressIndicator
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.emoji.api.packs.CustomImage
import io.element.android.libraries.emoji.impl.R
import io.element.android.libraries.matrix.api.media.MediaSource
import io.element.android.libraries.matrixmedia.api.MediaRequestData

@Composable
internal fun CustomEmojiGrid(
    images: List<CustomImage>,
    hasError: Boolean,
    onSelect: (String) -> Unit,
    selectedEmojis: Set<String> = emptySet(),
    isLoading: Boolean = false,
) {
    if (images.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(if (hasError) R.string.error_custom_emoji_load else R.string.screen_emoji_picker_no_custom_emoji))
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(images) { image -> CustomEmojiItem(image, image.url in selectedEmojis, onSelect) }
        }
    }
}

@Composable
internal fun CustomEmojiItem(image: CustomImage, isSelected: Boolean, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .border(2.dp, if (isSelected) ElementTheme.colors.iconAccentTertiary else Color.Transparent)
            .semantics { selected = isSelected }
            .clickable(role = Role.Button) { onSelect(image.url) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = MediaRequestData(MediaSource(image.url), MediaRequestData.Kind.Content),
            contentDescription = image.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp),
        )
        Text(image.shortcode, maxLines = 1, overflow = TextOverflow.Ellipsis, style = ElementTheme.typography.fontBodySmRegular)
    }
}

internal data class CustomEmojiPreviewState(val images: List<CustomImage>, val hasError: Boolean = false, val isLoading: Boolean = false)

internal class CustomEmojiPreviewParam : PreviewParameterProvider<CustomEmojiPreviewState> {
    override val values = sequenceOf(
        CustomEmojiPreviewState(listOf(CustomImage("turtle", "Turtle", "mxc://example.org/turtle", setOf("emoticon")))),
        CustomEmojiPreviewState(emptyList()),
        CustomEmojiPreviewState(emptyList(), hasError = true),
        CustomEmojiPreviewState(emptyList(), isLoading = true),
    )
}

@PreviewsDayNight
@Composable
internal fun CustomEmojiGridPreview(@PreviewParameter(CustomEmojiPreviewParam::class) state: CustomEmojiPreviewState) = ElementPreview {
    CustomEmojiGrid(state.images, state.hasError, {}, isLoading = state.isLoading)
}
