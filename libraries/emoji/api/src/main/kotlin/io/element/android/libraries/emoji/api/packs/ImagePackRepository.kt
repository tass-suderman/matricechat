package io.element.android.libraries.emoji.api.packs

import io.element.android.libraries.matrix.api.core.RoomId

/** Images shared through FluffyChat-compatible personal and globally enabled packs. */
data class CustomImage(
    val shortcode: String,
    val name: String,
    val url: String,
    val usage: Set<String>,
    val info: String? = null,
)

fun interface ImagePackRepository {
    suspend fun getImages(): Result<List<CustomImage>>

    suspend fun getImagesForRoom(roomId: RoomId?): Result<List<CustomImage>> = getImages()
}
