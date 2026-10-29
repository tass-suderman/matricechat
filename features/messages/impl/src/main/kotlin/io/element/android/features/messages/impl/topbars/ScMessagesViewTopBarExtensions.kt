package io.element.android.features.messages.impl.topbars

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.value
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.SharedHistoryIcon
import io.element.android.libraries.designsystem.components.avatar.Avatar
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.components.avatar.AvatarType
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.matrix.api.encryption.identity.IdentityState

@Composable
internal fun IdentityState?.scOverride() = when {
    ScPrefs.VERBOSE_CONVERSATION_ICONS.value() -> this
    this == null -> null
    this == IdentityState.Verified -> null
    // Even when verbose icons are disabled, still hide error states
    else -> this
}

@Composable
internal fun SharedHistoryIcon.scOverride() = when {
    ScPrefs.VERBOSE_CONVERSATION_ICONS.value() -> this
    else -> SharedHistoryIcon.NONE
}

@Composable
internal fun ScTitleAdditions(state: io.element.android.features.messages.impl.MessagesState) {
    ScBridgeAvatar(state)
    ScNotEncryptedIndicator(state.isRoomEncrypted)
}

@Composable
private fun ScNotEncryptedIndicator(isRoomEncrypted: Boolean?) {
    if (isRoomEncrypted == false && ScPrefs.SC_TIMELINE_LAYOUT.value()) {
        Icon(
            modifier = Modifier.size(14.dp),
            imageVector = CompoundIcons.LockOff(),
            contentDescription = null,
            tint = ElementTheme.colors.iconInfoPrimary,
        )
    }
}

@Composable
private fun ScBridgeAvatar(state: io.element.android.features.messages.impl.MessagesState?) {
    state?.bridgeState?.firstOrNull { it.protocol?.avatarUrl != null }?.protocol?.let { protocol ->
        protocol.avatarUrl?.let { bridgeAvatar ->
            Avatar(
                avatarData = AvatarData(
                    id = protocol.id ?: "",
                    name = protocol.displayName,
                    url = bridgeAvatar,
                    size = AvatarSize.InviteSender,
                ),
                avatarType = AvatarType.Sc(CircleShape),
            )
        }
    }
}
