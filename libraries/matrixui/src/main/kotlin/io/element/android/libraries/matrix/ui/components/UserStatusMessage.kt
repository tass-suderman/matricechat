package io.element.android.libraries.matrix.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.element.android.libraries.matrix.api.user.DisplayedStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

val LocalUserStatusMessage = staticCompositionLocalOf<(String) -> Flow<String?>> { { flowOf(null) } }

@Composable
fun rememberUserStatusMessage(userId: String?, displayedStatus: DisplayedStatus?): String? {
    val provider = LocalUserStatusMessage.current
    val presenceMessage by remember(userId, provider) { userId?.let(provider) ?: flowOf(null) }.collectAsState(null)
    return userStatusMessage(displayedStatus, presenceMessage)
}

internal fun userStatusMessage(displayedStatus: DisplayedStatus?, presenceMessage: String?): String? {
    val status = (displayedStatus as? DisplayedStatus.UserSet)?.status
    return status?.let { listOf(it.emoji, it.text).filter(String::isNotBlank).joinToString(" ").takeIf(String::isNotBlank) }
        ?: presenceMessage?.takeIf(String::isNotBlank)
}
