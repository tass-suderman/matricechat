/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.value
import chat.schildi.theme.ScTheme
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import java.io.File

val LocalTopBarOpacity = compositionLocalOf { 1f }
val LocalBottomBarOpacity = compositionLocalOf { 1f }

@Composable
fun Color.topBarBackground(): Color = copy(alpha = alpha * LocalTopBarOpacity.current)

@Composable
fun Color.bottomBarBackground(): Color = copy(alpha = alpha * LocalBottomBarOpacity.current)

/** Keep one continuous wallpaper behind the content and bar surfaces, without fading controls. */
@Composable
fun WallpaperBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val wallpaperEnabled = enabled && ScPrefs.CHAT_WALLPAPER_ENABLED.value()
    val wallpaper = ScPrefs.CHAT_WALLPAPER.value()
    val background = if (ScTheme.yes) MaterialTheme.colorScheme.background else ElementTheme.colors.bgCanvasDefault
    CompositionLocalProvider(
        LocalTopBarOpacity provides if (wallpaperEnabled) ScPrefs.TOP_BAR_OPACITY.value().coerceIn(0, 100) / 100f else 1f,
        LocalBottomBarOpacity provides if (wallpaperEnabled) ScPrefs.BOTTOM_BAR_OPACITY.value().coerceIn(0, 100) / 100f else 1f,
    ) {
        Box(modifier.background(background)) {
            if (wallpaperEnabled && wallpaper.isNotEmpty()) {
                AsyncImage(
                    model = File(wallpaper),
                    alpha = ScPrefs.CHAT_WALLPAPER_OPACITY.value().coerceIn(0, 100) / 100f,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            content()
        }
    }
}

internal class BarOpacityPreviewParam : androidx.compose.ui.tooling.preview.PreviewParameterProvider<Float> {
    override val values = sequenceOf(0f, 0.5f, 1f)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@io.element.android.libraries.designsystem.preview.PreviewsDayNight
@Composable
internal fun WallpaperBarsPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(BarOpacityPreviewParam::class) opacity: Float,
) = io.element.android.libraries.designsystem.preview.ElementPreview {
    CompositionLocalProvider(LocalTopBarOpacity provides opacity, LocalBottomBarOpacity provides opacity) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xff287bb5), Color(0xff9a579a))))
        ) {
            io.element.android.libraries.designsystem.theme.components.TopAppBar(titleStr = "Chat")
            io.element.android.libraries.designsystem.theme.components.Text(
                text = "Message composer",
                modifier = Modifier
                    .background(ElementTheme.colors.bgSubtleSecondary.bottomBarBackground())
                    .padding(16.dp),
            )
        }
    }
}
