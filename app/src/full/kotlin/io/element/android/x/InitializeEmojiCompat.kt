package io.element.android.x

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat

internal fun initializeEmojiCompat(context: Context) {
    EmojiCompat.init(BundledEmojiCompatConfig(context))
}
