package chat.schildi.components.preferences

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import chat.schildi.lib.R
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.designsystem.theme.components.Text
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun WallpaperPreference(current: String, onChange: (Any) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
            loading = true
            failed = false
            val result = withContext(Dispatchers.IO) {
                runCatchingExceptions {
                    val directory = File(context.filesDir, "wallpapers").apply { mkdirs() }
                    directory.listFiles()?.filter { it.path != current }?.forEach { it.delete() }
                    val destination = File(directory, UUID.randomUUID().toString())
                    try {
                        context.contentResolver.openInputStream(uri).use { input ->
                            requireNotNull(input)
                            destination.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    require(total <= 50L * 1024 * 1024)
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(destination.path, bounds)
                        require(bounds.outWidth > 0 && bounds.outHeight > 0)
                        destination.path
                    } catch (failure: Throwable) {
                        destination.delete()
                        throw failure
                    }
                }
            }
            result.onSuccess { onChange(it) }.onFailure { failed = true }
            loading = false
        }
        }
    }
    WallpaperPreferenceContent(
        current,
        loading,
        failed,
        onPick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onReset = { onChange("") },
    )
}

@Composable
private fun WallpaperPreferenceContent(current: String, loading: Boolean, failed: Boolean, onPick: () -> Unit, onReset: () -> Unit) {
    Column {
        io.element.android.libraries.designsystem.theme.components.ListItem(
            enabled = !loading,
            onClick = onPick,
            content = { Text(stringResource(R.string.screen_settings_chat_wallpaper_title)) },
            supportingContent = {
                Text(stringResource(if (failed) {
                R.string.error_chat_wallpaper_load
            } else if (current.isEmpty()) {
                R.string.screen_settings_chat_wallpaper_summary
            } else {
                R.string.screen_settings_chat_wallpaper_selected
            }))
            },
        )
        if (current.isNotEmpty()) {
            io.element.android.libraries.designsystem.theme.components.TextButton(
                text = stringResource(io.element.android.libraries.ui.strings.R.string.action_reset),
                onClick = onReset,
                enabled = !loading,
            )
        }
    }
}

internal data class WallpaperPreviewState(val current: String = "", val loading: Boolean = false, val failed: Boolean = false)

internal class WallpaperPreviewParam : androidx.compose.ui.tooling.preview.PreviewParameterProvider<WallpaperPreviewState> {
    override val values = sequenceOf(
        WallpaperPreviewState(),
        WallpaperPreviewState("wallpaper"),
        WallpaperPreviewState(loading = true),
        WallpaperPreviewState(failed = true)
    )
}

@io.element.android.libraries.designsystem.preview.PreviewsDayNight
@Composable
internal fun WallpaperPreferencePreview(@androidx.compose.ui.tooling.preview.PreviewParameter(WallpaperPreviewParam::class) state: WallpaperPreviewState) =
    io.element.android.libraries.designsystem.preview.ElementPreview {
        WallpaperPreferenceContent(state.current, state.loading, state.failed, {}, {})
    }

@Composable
internal fun WallpaperOpacityPreference(
    value: Int,
    enabled: Boolean,
    onChange: (Any) -> Unit,
    @androidx.annotation.StringRes titleRes: Int = R.string.screen_settings_chat_wallpaper_opacity_title,
) {
    io.element.android.libraries.designsystem.components.preferences.PreferenceSlide(
        title = stringResource(titleRes),
        summary = stringResource(R.string.screen_settings_chat_wallpaper_opacity_summary, value),
        value = value.coerceIn(0, 100) / 100f,
        onValueChange = { onChange(kotlin.math.round(it * 100).toInt().coerceIn(0, 100)) },
        enabled = enabled,
        steps = 99,
    )
}

internal data class WallpaperOpacityPreviewState(val value: Int, val enabled: Boolean = true)

internal class WallpaperOpacityPreviewParam : androidx.compose.ui.tooling.preview.PreviewParameterProvider<WallpaperOpacityPreviewState> {
    override val values = sequenceOf(
        WallpaperOpacityPreviewState(0), WallpaperOpacityPreviewState(50), WallpaperOpacityPreviewState(100),
        WallpaperOpacityPreviewState(50, enabled = false),
    )
}

@io.element.android.libraries.designsystem.preview.PreviewsDayNight
@Composable
internal fun WallpaperOpacityPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(WallpaperOpacityPreviewParam::class) state: WallpaperOpacityPreviewState,
) = io.element.android.libraries.designsystem.preview.ElementPreview {
    WallpaperOpacityPreference(state.value, state.enabled, {})
}
