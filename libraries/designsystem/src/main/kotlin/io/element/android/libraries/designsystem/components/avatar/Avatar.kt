/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.components.avatar.internal.InitialOrImageAvatar
import io.element.android.libraries.designsystem.components.avatar.internal.RoomAvatar
import io.element.android.libraries.designsystem.components.avatar.internal.SpaceAvatar
import io.element.android.libraries.designsystem.components.avatar.internal.UserAvatar
import io.element.android.libraries.designsystem.preview.ElementThemedPreview
import io.element.android.libraries.designsystem.preview.PreviewGroup
import io.element.android.libraries.designsystem.utils.CommonDrawables
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.sqrt

@Composable
fun Avatar(
    avatarData: AvatarData,
    avatarType: AvatarType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    // If not null, will be used instead of the size from avatarData
    forcedAvatarSize: Dp? = null,
    // If true, will show initials even if avatarData.url is not null
    hideImage: Boolean = false,
    showPresence: Boolean = true,
) {
    val presenceUserId = if (showPresence) {
        avatarData.presenceUserId ?: avatarData.id.takeIf { it.startsWith("@") }
            ?: (avatarType as? AvatarType.Room)?.heroes?.singleOrNull()?.id
    } else {
        null
    }
    val provider = LocalAvatarPresence.current
    val presence by remember(provider, presenceUserId) {
        presenceUserId?.let(provider) ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)
    val pictureSize = forcedAvatarSize ?: avatarData.size.dp
    val badgeSize = pictureSize * 0.28f
    // Centre the badge on the circle's 45-degree edge, within the picture's original square bounds.
    val badgeInset = pictureSize * ((1f - 1f / sqrt(2f)) / 2f) - badgeSize / 2
    androidx.compose.foundation.layout.Box(modifier) {
        when (avatarType) {
            is AvatarType.Sc -> InitialOrImageAvatar(
                avatarData = avatarData,
                hideAvatarImage = hideImage,
                avatarShape = avatarType.avatarShape(forcedAvatarSize ?: avatarData.size.dp),
                forcedAvatarSize = forcedAvatarSize,
                modifier = Modifier,
                contentDescription = contentDescription,
            )
            is AvatarType.Room -> RoomAvatar(
                avatarData = avatarData,
                avatarType = avatarType,
                modifier = Modifier,
                hideAvatarImage = hideImage,
                forcedAvatarSize = forcedAvatarSize,
                contentDescription = contentDescription,
            )
            AvatarType.User -> UserAvatar(
                avatarData = avatarData,
                modifier = Modifier,
                contentDescription = contentDescription,
                forcedAvatarSize = forcedAvatarSize,
                hideImage = hideImage,
            )
            is AvatarType.Space -> SpaceAvatar(
                avatarData = avatarData,
                avatarType = avatarType,
                modifier = Modifier,
                hideAvatarImage = hideImage,
                forcedAvatarSize = forcedAvatarSize,
                contentDescription = contentDescription,
            )
        }
        val color = when (presence) {
            "online" -> io.element.android.compound.theme.ElementTheme.colors.iconSuccessPrimary
            "unavailable" -> io.element.android.compound.theme.ElementTheme.colors.iconWarningPrimary
            "offline" -> io.element.android.compound.theme.ElementTheme.colors.iconSecondary
            else -> null
        }
        val presenceDescription = when (presence) {
            "online" -> androidx.compose.ui.res.stringResource(chat.schildi.lib.R.string.a11y_presence_online)
            "unavailable" -> androidx.compose.ui.res.stringResource(chat.schildi.lib.R.string.a11y_presence_unavailable)
            "offline" -> androidx.compose.ui.res.stringResource(chat.schildi.lib.R.string.a11y_presence_offline)
            else -> ""
        }
        if (color != null) {
            androidx.compose.foundation.layout.Box(
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = -badgeInset, y = -badgeInset)
                    .size(badgeSize)
                    .background(color, androidx.compose.foundation.shape.CircleShape)
                    .border(2.dp, io.element.android.compound.theme.ElementTheme.colors.bgCanvasDefault,
                        androidx.compose.foundation.shape.CircleShape)
                    .semantics { this.contentDescription = presenceDescription },
            )
        }
    }
}

@Preview(group = PreviewGroup.Avatars)
@Composable
internal fun AvatarPreview() = ElementThemedPreview(
    drawableFallbackForImages = CommonDrawables.sample_background,
) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            anAvatarData(size = AvatarSize.UserListItem),
            anAvatarData(size = AvatarSize.UserListItem, name = null),
            anAvatarData(size = AvatarSize.UserListItem, url = "aUrl"),
        ).forEach { avatarData ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.User,
                )
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.Room(isTombstoned = false),
                )
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.Room(
                        heroes = persistentListOf(
                            anAvatarData("@carol:server.org", "Carol", size = AvatarSize.UserListItem),
                            anAvatarData("@david:server.org", "David", size = AvatarSize.UserListItem),
                            anAvatarData("@eve:server.org", "Eve", size = AvatarSize.UserListItem),
                            anAvatarData("@justin:server.org", "Justin", size = AvatarSize.UserListItem),
                        )
                    )
                )
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.Room(isTombstoned = true),
                )
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.Space(isTombstoned = false),
                )
                Avatar(
                    avatarData = avatarData,
                    avatarType = AvatarType.Space(isTombstoned = true),
                )
            }
        }
    }
}
