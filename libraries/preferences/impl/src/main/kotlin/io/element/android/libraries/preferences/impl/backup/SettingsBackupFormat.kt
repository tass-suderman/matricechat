package io.element.android.libraries.preferences.impl.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.forEachPreference
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal const val MAX_BACKUP_BYTES = 80 * 1024 * 1024
internal val SESSION_STORE = Regex("session_[a-f0-9]{16}_preferences")
private val PUSH_STORE = Regex("push_store_[a-f0-9]{16}")
internal val GLOBAL_STORES = setOf(
    "schildinext-preferences",
    "schildinext-appstate",
    "elementx_preferences",
    "elementx_featureflag",
    "elementx_rageshake",
    "elementx_voice_player",
    "vector_analytics",
    "pin_code_store",
)
internal val ASSET_TARGETS = mapOf(
    "wallpaper" to ("schildinext-preferences" to "CHAT_WALLPAPER"),
    "message_sound" to ("elementx_preferences" to "notificationMessageSoundUri"),
    "call_sound" to ("elementx_preferences" to "notificationCallRingtoneUri"),
)

/** The format ID and preference IDs must remain stable across app renames. Add migrations when changing existing IDs/types. */
internal data class SettingsArchive(
    val stores: Map<String, Preferences>,
    val assets: Map<String, ByteArray> = emptyMap(),
    val distributors: Map<String, String> = emptyMap(),
    val locale: String? = null,
    val assetMimeTypes: Map<String, String> = emptyMap(),
    val unsetKeys: Map<String, Set<String>> = emptyMap(),
)

internal fun allowedStore(name: String) = name in GLOBAL_STORES || SESSION_STORE.matches(name) || PUSH_STORE.matches(name)

// Operational state and credentials sharing a store with settings are deliberately not portable.
internal fun allowedKey(store: String, key: String): Boolean = when {
    store == "schildinext-appstate" -> key == "SPACE_SELECTION"
    store == "vector_analytics" -> key in setOf("user_consent", "did_ask_user_consent")
    store == "pin_code_store" -> key == "biometric_unlock_enabled"
    PUSH_STORE.matches(store) -> key in setOf("notificationEnabled", "pushProviderName")
    SESSION_STORE.matches(store) -> key !in setOf("skipSessionVerification", "friendProfiles")
    else -> true
}

internal fun encodeValue(value: Any): JsonObject {
    val type = when (value) {
        is Boolean -> "boolean"
        is Int -> "int"
        is Long -> "long"
        is Float -> "float"
        is Double -> "double"
        is String -> "string"
        is Set<*> -> "stringSet"
        else -> error("Unsupported setting type")
    }
    val encoded = if (value is Set<*>) JsonArray(value.map { JsonPrimitive(it as String) }.sortedBy { it.content }) else JsonPrimitive(value.toString())
    return buildJsonObject {
        put("type", type)
        put("value", encoded)
    }
}

internal fun decodePreferences(values: JsonObject): Preferences = mutablePreferencesOf().apply {
    values.forEach { (key, element) ->
        require(key.length in 1..256)
        val item = element.jsonObject
        val type = item.getValue("type").jsonPrimitive.content
        val value = item.getValue("value")
        when (type) {
            "boolean" -> this[booleanPreferencesKey(key)] = value.jsonPrimitive.content.toBooleanStrict()
            "int" -> this[intPreferencesKey(key)] = value.jsonPrimitive.content.toInt()
            "long" -> this[longPreferencesKey(key)] = value.jsonPrimitive.content.toLong()
            "float" -> this[floatPreferencesKey(key)] = value.jsonPrimitive.content.toFloat().also { require(it.isFinite()) }
            "double" -> this[doublePreferencesKey(key)] = value.jsonPrimitive.content.toDouble().also { require(it.isFinite()) }
            "string" -> this[stringPreferencesKey(key)] = value.jsonPrimitive.content
            "stringSet" -> this[stringSetPreferencesKey(key)] = value.jsonArray.map { it.jsonPrimitive.content }.toSet()
            else -> Unit // A newer exporter can introduce value types this version does not understand.
        }
    }
}.toPreferences()

internal fun SettingsArchive.writeTo(output: OutputStream) {
    val manifest = buildJsonObject {
        put("format", "matrix-client-settings")
        put("version", 1)
        put("stores", buildJsonObject {
            stores.forEach { (name, preferences) ->
                put(name, buildJsonObject {
                    preferences.asMap().forEach { (key, value) -> if (allowedKey(name, key.name)) put(key.name, encodeValue(value)) }
                    unsetKeys[name].orEmpty().filter { allowedKey(name, it) }.forEach { key ->
                        put(key, buildJsonObject {
                            put("type", "unset")
                            put("value", JsonNull)
                        })
                    }
                })
            }
        })
        put("distributors", buildJsonObject { distributors.forEach { (key, value) -> put(key, value) } })
        locale?.let { put("locale", it) }
        put("assetMimeTypes", buildJsonObject { assetMimeTypes.forEach { (name, mime) -> put(name, mime) } })
    }
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("settings.json"))
        zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        assets.forEach { (name, bytes) ->
            require(name in ASSET_TARGETS)
            zip.putNextEntry(ZipEntry("assets/$name"))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
}

internal fun InputStream.readBounded(limit: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return out.toByteArray()
        require(out.size().toLong() + count <= limit) { "Settings backup is too large" }
        out.write(buffer, 0, count)
    }
}

internal fun readSettingsArchive(input: InputStream): SettingsArchive {
    val entries = mutableMapOf<String, ByteArray>()
    var total = 0
    var count = 0
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            require(++count <= 16 && !entries.containsKey(entry.name))
            val bytes = zip.readBounded(MAX_BACKUP_BYTES - total)
            total += bytes.size
            if (entry.name == "settings.json") require(bytes.size <= 2 * 1024 * 1024)
            entries[entry.name] = bytes
        }
    }
    val manifest = Json.parseToJsonElement(entries.getValue("settings.json").toString(Charsets.UTF_8)).jsonObject
    require(manifest["format"]?.jsonPrimitive?.content == "matrix-client-settings")
    require(manifest["version"]?.jsonPrimitive?.int == 1) { "Unsupported settings backup version" }
    val knownStores = manifest.getValue("stores").jsonObject.filterKeys(::allowedStore)
    val unsetKeys = knownStores.mapValues { (name, values) ->
        values.jsonObject.filter { (key, entry) ->
            allowedKey(name, key) && entry.jsonObject["type"]?.jsonPrimitive?.content == "unset"
        }.keys
    }
    val stores = knownStores.mapValues { (name, value) ->
        decodePreferences(JsonObject(value.jsonObject.filterKeys { allowedKey(name, it) }))
    }
    val distributors = manifest["distributors"]?.jsonObject.orEmpty().filterKeys { it.startsWith("DISTRIBUTOR@") }
        .mapValues { it.value.jsonPrimitive.content }
    return SettingsArchive(
        stores = stores,
        assets = ASSET_TARGETS.keys.mapNotNull { name -> entries["assets/$name"]?.let { name to it } }.toMap(),
        distributors = distributors,
        locale = manifest["locale"]?.jsonPrimitive?.contentOrNull,
        assetMimeTypes = manifest["assetMimeTypes"]?.jsonObject.orEmpty().filterKeys { it in ASSET_TARGETS }
            .mapValues { it.value.jsonPrimitive.content },
        unsetKeys = unsetKeys,
    )
}

internal fun validateTypes(name: String, imported: Preferences, current: Preferences) {
    val expected = current.asMap().mapKeys { it.key.name }.mapValues { encodeValue(it.value).getValue("type").jsonPrimitive.content }.toMutableMap()
    if (name == "schildinext-preferences") {
        ScPrefs.scTweaks.forEachPreference { pref ->
            pref.key?.let { expected[it.name] = encodeValue(pref.defaultValue as Any).getValue("type").jsonPrimitive.content }
        }
    }
    if (SESSION_STORE.matches(name)) {
        listOf("sharePresence", "sendPublicReadReceipts", "renderReadReceipts", "sendTypingNotifications", "renderTypingNotifications", "compressMedia")
            .forEach { expected[it] = "boolean" }
        expected["compressMediaPreset"] = "string"
    }
    if (name == "elementx_preferences") {
        listOf("developerMode", "hideInviteAvatars", "otherAccountsExpanded").forEach { expected[it] = "boolean" }
        listOf("liveLocationMinimumDistanceUpdate", "notificationMessageSoundChannelVersion", "notificationCallRingtoneChannelVersion")
            .forEach { expected[it] = "int" }
        listOf(
            "elementCallBaseUrl",
            "theme",
            "timelineMediaPreviewValue",
            "logLevel",
            "traceLogPacks",
            "homeserverHistory",
            "notificationMessageSoundUri",
            "notificationMessageSoundDisplayName",
            "notificationCallRingtoneUri",
            "notificationCallRingtoneDisplayName"
        )
            .forEach { expected[it] = "string" }
    }
    imported.asMap().forEach { (key, value) ->
        val type = encodeValue(value).getValue("type").jsonPrimitive.content
        val required = when {
            name == "elementx_featureflag" || name == "vector_analytics" || name == "pin_code_store" -> "boolean"
            name == "elementx_voice_player" -> "int"
            name == "elementx_rageshake" -> if (key.name == "enabled") "boolean" else "float"
            name == "schildinext-appstate" -> "string"
            PUSH_STORE.matches(name) -> if (key.name == "notificationEnabled") "boolean" else "string"
            else -> expected[key.name]
        }
        require(required == null || required == type) { "Invalid setting value type" }
    }
}

internal fun MutablePreferences.mergeImported(imported: Preferences) {
    imported.asMap().forEach { (key, value) ->
        @Suppress("UNCHECKED_CAST")
        set(key as Preferences.Key<Any>, value)
    }
}

internal fun sanitiseImported(name: String, imported: Preferences): Preferences = imported.toMutablePreferences().apply {
    if (name == "elementx_preferences") {
        val logLevel = stringPreferencesKey("logLevel")
        if (this[logLevel] !in io.element.android.libraries.matrix.api.tracing.LogLevel.entries.map { it.name }) remove(logLevel)
        val mediaPreview = stringPreferencesKey("timelineMediaPreviewValue")
        if (this[mediaPreview] !in io.element.android.libraries.matrix.api.media.MediaPreviewValue.entries.map { it.name }) remove(mediaPreview)
    }
    if (name == "schildinext-preferences") {
        ScPrefs.scTweaks.forEachPreference { pref ->
            if (pref is chat.schildi.lib.preferences.ScIntPref) {
                this[pref.key]?.let { this[pref.key] = it.coerceIn(pref.minValue, pref.maxValue) }
            }
        }
    }
}.toPreferences()

internal fun MutablePreferences.removeImported(keys: Set<String>) {
    keys.forEach { remove(stringPreferencesKey(it)) }
}
