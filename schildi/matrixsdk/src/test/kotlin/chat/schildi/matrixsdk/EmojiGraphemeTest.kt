package chat.schildi.matrixsdk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiGraphemeTest {
    @Test
    fun `emoji sequences remain valid without a bundled font`() {
        listOf("❤️", "👨‍👩‍👦", "🇫🇷", "👩🏿‍🔧", "1️⃣", "🌍").forEach {
            assertTrue(it, it.containsOnlyEmojiGraphemes(1))
        }
    }

    @Test
    fun `emoji limits count sequences rather than code points`() {
        assertTrue("👨‍👩‍👦❤️".containsOnlyEmojiGraphemes(2))
        assertFalse("👨‍👩‍👦❤️".containsOnlyEmojiGraphemes(1))
        assertFalse("❤️".containsOnlyEmojiGraphemes(0))
    }

    @Test
    fun `text and gaps cannot enter recent emoji`() {
        listOf("", "hello", "a❤️", "❤️a", "❤️ ❤️", "1").forEach {
            assertFalse(it, it.containsOnlyEmojiGraphemes(10))
        }
    }
}
