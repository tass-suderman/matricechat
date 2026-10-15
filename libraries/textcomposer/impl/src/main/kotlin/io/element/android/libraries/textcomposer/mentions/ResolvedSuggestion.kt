/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.textcomposer.mentions

import androidx.compose.runtime.Immutable
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.RoomAlias
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.slashcommands.api.SlashCommandSuggestion

@Immutable
sealed interface ResolvedSuggestion {
    data class Emoji(
        val name: String,
        val shortcode: String,
        val unicode: String? = null,
        val imageUrl: String? = null,
        val aliases: List<String> = emptyList(),
    ) : ResolvedSuggestion {
        val text: String get() = unicode ?: ":$shortcode:"
    }

    data object AtRoom : ResolvedSuggestion
    data class Member(val roomMember: RoomMember) : ResolvedSuggestion
    data class Alias(
        val roomAlias: RoomAlias,
        val roomId: RoomId,
        val roomName: String?,
        val roomAvatarUrl: String?,
    ) : ResolvedSuggestion {
        fun getAvatarData(size: AvatarSize) = AvatarData(
            id = roomId.value,
            name = roomName,
            url = roomAvatarUrl,
            size = size,
        )
    }

    data class Command(
        val command: SlashCommandSuggestion,
    ) : ResolvedSuggestion
}
