package io.element.android.libraries.emoji.api

import io.element.android.emojibasebindings.Emoji
import kotlinx.collections.immutable.ImmutableList

fun interface GetEmojis {
    suspend operator fun invoke(): ImmutableList<Emoji>
}
