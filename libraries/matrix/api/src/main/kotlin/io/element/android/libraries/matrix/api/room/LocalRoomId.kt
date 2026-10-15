package io.element.android.libraries.matrix.api.room

import androidx.compose.runtime.staticCompositionLocalOf
import io.element.android.libraries.matrix.api.core.RoomId

/** Room whose composer and timeline are currently being displayed. */
val LocalRoomId = staticCompositionLocalOf<RoomId?> { null }
