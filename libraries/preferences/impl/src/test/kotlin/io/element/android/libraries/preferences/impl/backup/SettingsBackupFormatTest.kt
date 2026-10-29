package io.element.android.libraries.preferences.impl.backup

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SettingsBackupFormatTest {
    @Test
    fun `all preference types and binary assets survive a round trip`() {
        val prefs = preferencesOf(
            booleanPreferencesKey("boolean") to true,
            intPreferencesKey("argb") to 0xff123456.toInt(),
            longPreferencesKey("large") to Long.MAX_VALUE,
            floatPreferencesKey("float") to 0.125f,
            doublePreferencesKey("double") to 1.23456789,
            stringPreferencesKey("unicode") to "🐢 café",
            stringSetPreferencesKey("set") to setOf("one", "two"),
        )
        val archive = SettingsArchive(
            stores = mapOf("elementx_preferences" to prefs),
            assets = mapOf("message_sound" to byteArrayOf(1, 2, 3)),
            distributors = mapOf("DISTRIBUTOR@alice:example.org" to "org.example.distributor"),
            locale = "en-GB",
            assetMimeTypes = mapOf("message_sound" to "audio/ogg"),
        )
        val restored = roundTrip(archive)
        assertThat(restored.stores).isEqualTo(archive.stores)
        assertThat(restored.assets.getValue("message_sound")).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(restored.distributors).isEqualTo(archive.distributors)
        assertThat(restored.assetMimeTypes).isEqualTo(archive.assetMimeTypes)
        assertThat(restored.locale).isEqualTo("en-GB")
    }

    @Test
    fun `older exports merge without clearing settings added later`() {
        val old = roundTrip(SettingsArchive(mapOf("elementx_preferences" to preferencesOf(stringPreferencesKey("theme") to "dark"))))
        val target = mutablePreferencesOf(stringPreferencesKey("theme") to "light", booleanPreferencesKey("new_setting") to false)
        target.mergeImported(old.stores.getValue("elementx_preferences"))
        assertThat(target[stringPreferencesKey("theme")]).isEqualTo("dark")
        assertThat(target[booleanPreferencesKey("new_setting")]).isFalse()
    }

    @Test
    fun `missing optional fields and unknown additions do not block older format imports`() {
        val archive =
            manifest("""{"format":"matrix-client-settings","version":1,"futureField":{},"stores":{
            "future_store":{"future":{}},
            "elementx_preferences":{"theme":{"type":"string","value":"dark"},"future":{"type":"future","value":{}}}
        }}""")
        val restored = readSettingsArchive(archive.inputStream())
        assertThat(restored.stores.keys).containsExactly("elementx_preferences")
        assertThat(restored.stores.getValue("elementx_preferences")[stringPreferencesKey("theme")]).isEqualTo("dark")
        assertThat(restored.assets).isEmpty()
    }

    @Test
    fun `private friend profiles are not included in portable settings`() {
        val store = "session_0123456789abcdef_preferences"
        val preferences = preferencesOf(
            stringPreferencesKey("friendProfiles") to "private local nicknames",
            booleanPreferencesKey("sendPublicReadReceipts") to false,
        )
        val restored = roundTrip(SettingsArchive(mapOf(store to preferences)))
        assertThat(restored.stores.getValue(store)[stringPreferencesKey("friendProfiles")]).isNull()
        assertThat(restored.stores.getValue(store)[booleanPreferencesKey("sendPublicReadReceipts")]).isFalse()
    }

    @Test
    fun `raw disabled tweak overrides are retained`() {
        val prefs = preferencesOf(booleanPreferencesKey("CUSTOM_COLORS") to false, intPreferencesKey("ACCENT_DARK") to -1234)
        assertThat(roundTrip(SettingsArchive(mapOf("schildinext-preferences" to prefs))).stores.values.single()).isEqualTo(prefs)
    }

    @Test
    fun `account settings retain stable identity independently of package name`() {
        val store = "session_0123456789abcdef_preferences"
        val prefs = preferencesOf(booleanPreferencesKey("sendPublicReadReceipts") to false)
        assertThat(roundTrip(SettingsArchive(mapOf(store to prefs))).stores[store]).isEqualTo(prefs)
    }

    @Test
    fun `credentials and operational state sharing preference stores are excluded`() {
        val archive = SettingsArchive(mapOf(
            "pin_code_store" to preferencesOf(stringPreferencesKey("encoded_pin_code") to "secret", booleanPreferencesKey("biometric_unlock_enabled") to true),
            "vector_analytics" to preferencesOf(stringPreferencesKey("analytics_id") to "identity", booleanPreferencesKey("user_consent") to false),
            "push_store_0123456789abcdef" to
                preferencesOf(stringPreferencesKey("currentPushKey") to "secret", booleanPreferencesKey("notificationEnabled") to false),
        ))
        val restored = roundTrip(archive)
        assertThat(restored.stores.values.flatMap { it.asMap().keys.map { key -> key.name } })
            .containsExactly("biometric_unlock_enabled", "user_consent", "notificationEnabled")
        assertThat(allowedStore("../../sessions")).isFalse()
        assertThat(allowedStore("push_client_secret_store")).isFalse()
    }

    @Test
    fun `wrong known value types are rejected before application`() {
        assertThrows(IllegalArgumentException::class.java) {
            validateTypes("elementx_preferences", preferencesOf(booleanPreferencesKey("theme") to true), emptyPreferences())
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateTypes("session_0123456789abcdef_preferences", preferencesOf(stringPreferencesKey("sharePresence") to "yes"), emptyPreferences())
        }
    }

    @Test
    fun `unsupported format versions and broken manifests are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            readSettingsArchive(manifest("""{"format":"matrix-client-settings","version":2,"stores":{}}""").inputStream())
        }
        assertThrows(IllegalArgumentException::class.java) {
            readSettingsArchive(manifest("""{"format":"other-app","version":1,"stores":{}}""").inputStream())
        }
        assertThrows(Exception::class.java) { readSettingsArchive(byteArrayOf(1, 2, 3).inputStream()) }
    }

    @Test
    fun `excess ZIP entry counts and oversized reads are rejected`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            repeat(17) { index ->
                zip.putNextEntry(ZipEntry("entry-$index"))
                zip.write(0)
                zip.closeEntry()
            }
        }
        assertThrows(IllegalArgumentException::class.java) { readSettingsArchive(out.toByteArray().inputStream()) }
        assertThrows(IllegalArgumentException::class.java) { ByteArrayInputStream(ByteArray(11)).readBounded(10) }
    }

    @Test
    fun `removed enum values leave the current choice intact`() {
        val target = mutablePreferencesOf(stringPreferencesKey("logLevel") to "INFO")
        val imported = preferencesOf(stringPreferencesKey("logLevel") to "REMOVED_LOG_LEVEL")
        target.mergeImported(sanitiseImported("elementx_preferences", imported))
        assertThat(target[stringPreferencesKey("logLevel")]).isEqualTo("INFO")
    }

    @Test
    fun `wallpaper opacity is portable and older backups leave its current value intact`() {
        val opacity = intPreferencesKey("CHAT_WALLPAPER_OPACITY")
        val original = preferencesOf(opacity to 37)
        val restored = roundTrip(SettingsArchive(mapOf("schildinext-preferences" to original)))
        assertThat(restored.stores.getValue("schildinext-preferences")[opacity]).isEqualTo(37)
        val target = mutablePreferencesOf(opacity to 23)
        target.mergeImported(preferencesOf(booleanPreferencesKey("CUSTOM_COLORS") to false))
        assertThat(target[opacity]).isEqualTo(23)
        assertThat(sanitiseImported("schildinext-preferences", preferencesOf(opacity to 500))[opacity]).isEqualTo(100)
        assertThat(sanitiseImported("schildinext-preferences", preferencesOf(opacity to -1))[opacity]).isEqualTo(0)
    }

    @Test
    fun `explicit unset entries restore defaults but missing old keys preserve current values`() {
        val archive = roundTrip(SettingsArchive(
            stores = mapOf("elementx_preferences" to emptyPreferences()),
            unsetKeys = mapOf("elementx_preferences" to setOf("notificationMessageSoundUri", "hideInviteAvatars")),
        ))
        val target = mutablePreferencesOf(
            stringPreferencesKey("notificationMessageSoundUri") to "custom-sound",
            booleanPreferencesKey("hideInviteAvatars") to true,
            booleanPreferencesKey("new_setting") to true,
        )
        target.removeImported(archive.unsetKeys.getValue("elementx_preferences"))
        target.mergeImported(archive.stores.getValue("elementx_preferences"))
        assertThat(target[stringPreferencesKey("notificationMessageSoundUri")]).isNull()
        assertThat(target[booleanPreferencesKey("hideInviteAvatars")]).isNull()
        assertThat(target[booleanPreferencesKey("new_setting")]).isTrue()
    }

    private fun roundTrip(archive: SettingsArchive): SettingsArchive {
        val out = ByteArrayOutputStream()
        archive.writeTo(out)
        return readSettingsArchive(out.toByteArray().inputStream())
    }

    private fun manifest(json: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("settings.json"))
            zip.write(json.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }
}
