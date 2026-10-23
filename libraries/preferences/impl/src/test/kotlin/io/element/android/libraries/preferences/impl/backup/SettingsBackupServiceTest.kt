package io.element.android.libraries.preferences.impl.backup

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import chat.schildi.lib.preferences.DefaultScAppStateStore
import chat.schildi.lib.preferences.DefaultScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.preferences.impl.store.DefaultPreferencesDataStoreFactory
import io.element.android.libraries.preferences.impl.store.DefaultSessionPreferencesStore
import io.element.android.libraries.preferences.test.InMemoryAppPreferencesStore
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Config(sdk = [36], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class SettingsBackupServiceTest {
    @Test
    fun `export and restore portable settings and wallpaper with validation and rollback`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = DefaultPreferencesDataStoreFactory(context)
        val tweaks = DefaultScPreferencesStore(context)
        val appState = DefaultScAppStateStore(context)
        val shared = context.getSharedPreferences("backup-test", Context.MODE_PRIVATE)
        val service = DefaultSettingsBackupService(context, factory, tweaks, appState, shared, CoroutineDispatchers.Default, InMemoryAppPreferencesStore())
        val global = factory.create("elementx_preferences")
        global.edit { it[stringPreferencesKey("theme")] = "dark" }
        tweaks.setSetting(ScPrefs.CHAT_WALLPAPER_OPACITY, 37)
        tweaks.setSetting(ScPrefs.TOP_BAR_OPACITY, 25)
        tweaks.setSetting(ScPrefs.BOTTOM_BAR_OPACITY, 75)
        tweaks.setSetting(ScPrefs.HIDE_MISMATCHED_SENDER_WARNING, true)
        tweaks.setSetting(ScPrefs.CUSTOM_COLORS, false)
        tweaks.setSetting(ScPrefs.ACCENT_DARK, 0xff123456.toInt())
        tweaks.setSetting(ScPrefs.CHAT_ACCENT, 0xffbc27de.toInt())
        val wallpaper = File(context.filesDir, "source-wallpaper.png")
        Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888).let { bitmap ->
            wallpaper.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        tweaks.setSetting(ScPrefs.CHAT_WALLPAPER, wallpaper.path)
        val sessionId = SessionId("@backup:example.org")
        val sessionName = DefaultSessionPreferencesStore.storeFile(context, sessionId).name.removeSuffix(".preferences_pb")
        factory.create(sessionName).edit { it[booleanPreferencesKey("sendPublicReadReceipts")] = false }
        shared.edit().putString("DISTRIBUTOR@backup:example.org", "org.example.distributor").commit()
        val file = File(context.cacheDir, "settings-export.zip")
        service.exportSettings(Uri.fromFile(file)).getOrThrow()
        assertThat(file.length()).isGreaterThan(0)
        global.edit {
            it[stringPreferencesKey("theme")] = "light"
            it[booleanPreferencesKey("added_after_export")] = true
            it[stringPreferencesKey("notificationMessageSoundUri")] = "content://previous-app/sound"
        }
        tweaks.setSetting(ScPrefs.CHAT_WALLPAPER_OPACITY, 100)
        tweaks.setSetting(ScPrefs.TOP_BAR_OPACITY, 100)
        tweaks.setSetting(ScPrefs.BOTTOM_BAR_OPACITY, 100)
        tweaks.setSetting(ScPrefs.HIDE_MISMATCHED_SENDER_WARNING, false)
        tweaks.setSetting(ScPrefs.CUSTOM_COLORS, true)
        tweaks.setSetting(ScPrefs.ACCENT_DARK, 0)
        tweaks.setSetting(ScPrefs.CHAT_ACCENT, 0)
        tweaks.setSetting(ScPrefs.CHAT_WALLPAPER, "")
        factory.create(sessionName).edit { it[booleanPreferencesKey("sendPublicReadReceipts")] = true }
        service.importSettings(Uri.fromFile(file)).getOrThrow()
        assertThat(global.data.first()[stringPreferencesKey("theme")]).isEqualTo("dark")
        assertThat(global.data.first()[booleanPreferencesKey("added_after_export")]).isTrue()
        assertThat(global.data.first()[stringPreferencesKey("notificationMessageSoundUri")]).isNull()
        assertThat(tweaks.getSetting(ScPrefs.CHAT_WALLPAPER_OPACITY)).isEqualTo(37)
        assertThat(tweaks.getSetting(ScPrefs.TOP_BAR_OPACITY)).isEqualTo(25)
        assertThat(tweaks.getSetting(ScPrefs.BOTTOM_BAR_OPACITY)).isEqualTo(75)
        assertThat(tweaks.getSetting(ScPrefs.HIDE_MISMATCHED_SENDER_WARNING)).isTrue()
        assertThat(tweaks.getSetting(ScPrefs.CUSTOM_COLORS)).isFalse()
        assertThat(tweaks.backupDataStore.data.first()[ScPrefs.ACCENT_DARK.key]).isEqualTo(0xff123456.toInt())
        assertThat(tweaks.backupDataStore.data.first()[ScPrefs.CHAT_ACCENT.key]).isEqualTo(0xffbc27de.toInt())
        val restoredWallpaper = File(tweaks.getSetting(ScPrefs.CHAT_WALLPAPER))
        assertThat(restoredWallpaper.path).isNotEqualTo(wallpaper.path)
        assertThat(restoredWallpaper.readBytes()).isEqualTo(wallpaper.readBytes())
        // Opening the account after import reuses the same DataStore instead of losing or duplicating its restored preferences.
        val session = DefaultSessionPreferencesStore(context, sessionId, backgroundScope, factory.create(sessionName))
        assertThat(session.isSendPublicReadReceiptsEnabled().first()).isFalse()
        assertThat(factory.create(sessionName)).isSameInstanceAs(factory.create(sessionName))

        val bad = File(context.cacheDir, "wrong-type.zip")
        val output = ByteArrayOutputStream()
        SettingsArchive(mapOf("elementx_preferences" to androidx.datastore.preferences.core.preferencesOf(
            booleanPreferencesKey("theme") to true,
        ))).writeTo(output)
        bad.writeBytes(output.toByteArray())
        assertThat(service.importSettings(Uri.fromFile(bad)).isFailure).isTrue()
        assertThat(global.data.first()[stringPreferencesKey("theme")]).isEqualTo("dark")

        global.edit { it[stringPreferencesKey("theme")] = "light" }
        val previousFiles = File(context.filesDir, "wallpapers").listFiles().orEmpty().map { it.name }.toSet()
        val failingShared = object : SharedPreferences by shared {
            override fun edit(): SharedPreferences.Editor {
                val real = shared.edit()
                return object : SharedPreferences.Editor by real { override fun commit() = false }
            }
        }
        val failingService = DefaultSettingsBackupService(context, factory, tweaks, appState, failingShared, CoroutineDispatchers.Default, InMemoryAppPreferencesStore())
        assertThat(failingService.importSettings(Uri.fromFile(file)).isFailure).isTrue()
        assertThat(global.data.first()[stringPreferencesKey("theme")]).isEqualTo("light")
        assertThat(tweaks.getSetting(ScPrefs.CHAT_WALLPAPER)).isEqualTo(restoredWallpaper.path)
        assertThat(File(context.filesDir, "wallpapers").listFiles().orEmpty().map { it.name }.toSet()).isEqualTo(previousFiles)
    }
}
