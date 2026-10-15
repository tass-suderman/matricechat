package io.element.android.libraries.matrix.ui.messages.sender

import androidx.compose.runtime.staticCompositionLocalOf

data class DiscordRole(val name: String, val color: Long)

val LocalDiscordRoles = staticCompositionLocalOf<Map<String, DiscordRole>> { emptyMap() }
