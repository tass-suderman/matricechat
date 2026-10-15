package io.element.android.libraries.emoji.impl

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import io.element.android.libraries.emoji.api.GetEmojis

@Inject
@ContributesBinding(AppScope::class)
class DefaultGetEmojis(private val provider: EmojibaseProvider) : GetEmojis {
    override suspend fun invoke() = provider.getStore().allEmojis
}
