package io.element.android.libraries.designsystem.components.avatar

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

val LocalAvatarPresence = staticCompositionLocalOf<(String) -> Flow<String?>> { { flowOf(null) } }

internal class AvatarPresencePreviewParam : androidx.compose.ui.tooling.preview.PreviewParameterProvider<String?> {
    override val values = sequenceOf(null, "online", "unavailable", "offline")
}

@io.element.android.libraries.designsystem.preview.PreviewsDayNight
@androidx.compose.runtime.Composable
internal fun AvatarPresencePreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(AvatarPresencePreviewParam::class) status: String?,
) = io.element.android.libraries.designsystem.preview.ElementPreview {
    androidx.compose.runtime.CompositionLocalProvider(LocalAvatarPresence provides { flowOf(status) }) {
        Avatar(anAvatarData(id = "@user:example.org", size = AvatarSize.UserListItem), AvatarType.User)
    }
}
