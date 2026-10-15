package io.element.android.libraries.preferences.impl.backup

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import chat.schildi.lib.preferences.ScAppStateStore
import chat.schildi.lib.preferences.ScPreferencesStore
import chat.schildi.lib.preferences.ScPrefs
import chat.schildi.lib.preferences.forEachPreference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.di.annotations.ApplicationContext
import io.element.android.libraries.preferences.api.SettingsBackupService
import io.element.android.libraries.preferences.api.store.AppPreferencesStore
import io.element.android.libraries.preferences.api.store.NotificationSound.Companion.toStored
import io.element.android.libraries.preferences.api.store.PreferenceDataStoreFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class DefaultSettingsBackupService(
    @ApplicationContext private val context: Context,
    private val factory: PreferenceDataStoreFactory,
    private val tweaks: ScPreferencesStore,
    private val appState: ScAppStateStore,
    private val sharedPreferences: SharedPreferences,
    private val dispatchers: CoroutineDispatchers,
    private val appPreferences: AppPreferencesStore,
) : SettingsBackupService {
    private val mutex = Mutex()

    private fun store(name: String): DataStore<Preferences> = when (name) {
        "schildinext-preferences" -> tweaks.backupDataStore
        "schildinext-appstate" -> appState.store
        else -> factory.create(name)
    }

    override suspend fun exportSettings(destination: Uri): Result<Unit> = withContext(dispatchers.io) {
        runCatchingExceptions {
            mutex.withLock {
                val names = GLOBAL_STORES + File(context.filesDir, "datastore").listFiles().orEmpty()
                    .map { it.name.removeSuffix(".preferences_pb") }.filter(::allowedStore)
                val stores = names.associateWith { name -> store(name).data.first() }.toMutableMap()
                val unsetKeys = completeSnapshot(stores)
                val sc = stores.getValue("schildinext-preferences").toMutablePreferences()
                // Export raw overrides, including disabled options, and explicit defaults for every Turtle tweak.
                ScPrefs.scTweaks.forEachPreference { pref ->
                    @Suppress("UNCHECKED_CAST")
                    val key = pref.key as? Preferences.Key<Any>
                    if (key != null && key !in sc) sc[key] = pref.defaultValue as Any
                }
                stores["schildinext-preferences"] = sc.toPreferences()
                val assets = mutableMapOf<String, ByteArray>()
                val mimeTypes = mutableMapOf<String, String>()
                ASSET_TARGETS.forEach { (name, target) ->
                    val value = stores.getValue(target.first)[stringPreferencesKey(target.second)].orEmpty()
                    if (value.isNotEmpty() && (name == "wallpaper" || value.startsWith("content:") || value.startsWith("file:"))) {
                        val uri = if (name == "wallpaper") Uri.fromFile(File(value)) else Uri.parse(value)
                        assets[name] = requireNotNull(context.contentResolver.openInputStream(uri)).use {
                            it.readBounded(if (name == "wallpaper") 50 * 1024 * 1024 else 10 * 1024 * 1024)
                        }
                        mimeTypes[name] = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    }
                }
                val distributors = sharedPreferences.all.filterKeys { it.startsWith("DISTRIBUTOR@") }
                    .mapValues { it.value as String }
                val locale = if (Build.VERSION.SDK_INT >= 33) {
                    context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
                } else null
                val archive = SettingsArchive(stores, assets, distributors, locale, mimeTypes, unsetKeys)
                val temporary = File.createTempFile("settings-", ".zip", context.cacheDir)
                try {
                    temporary.outputStream().use { archive.writeTo(it) }
                    requireNotNull(context.contentResolver.openOutputStream(destination, "wt")).use { output ->
                        temporary.inputStream().use { it.copyTo(output) }
                    }
                } finally {
                    temporary.delete()
                }
                Unit
            }
        }
    }

    override suspend fun importSettings(source: Uri): Result<Unit> = withContext(dispatchers.io) {
        runCatchingExceptions {
            mutex.withLock {
                val archive = requireNotNull(context.contentResolver.openInputStream(source)).use(::readSettingsArchive)
                val previous = archive.stores.mapValues { (name, _) -> store(name).data.first() }
                archive.stores.forEach { (name, preferences) -> validateTypes(name, preferences, previous.getValue(name)) }
                validateAssetReferences(archive)
                val prepared = archive.stores.mapValues { (name, preferences) -> sanitiseImported(name, preferences).toMutablePreferences() }
                val files = mutableListOf<File>()
                val oldDistributors = sharedPreferences.all.filterKeys { it in archive.distributors }.mapValues { it.value as String }
                // Theme/locale changes can recreate the activity. Finish the transaction even if its screen is disposed.
                withContext(NonCancellable) {
                    val localeManager = if (Build.VERSION.SDK_INT >= 33) context.getSystemService(LocaleManager::class.java) else null
                    val previousLocale = if (Build.VERSION.SDK_INT >= 33) localeManager?.applicationLocales else null
                    try {
                        archive.assets.forEach { (name, bytes) ->
                            val target = ASSET_TARGETS.getValue(name)
                            val directory = File(context.filesDir, if (name == "wallpaper") "wallpapers" else "notification_sounds").apply { mkdirs() }
                            val extension = archive.assetMimeTypes[name]?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                                ?.takeIf { it.matches(Regex("[a-zA-Z0-9]{1,8}")) } ?: "bin"
                            val file = File(directory, "${UUID.randomUUID()}.$extension")
                            files += file
                            file.writeBytes(bytes)
                            val value = if (name == "wallpaper") file.path else {
                                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
                            }
                            prepared.getValue(target.first)[stringPreferencesKey(target.second)] = value
                        }
                        prepared["elementx_preferences"]?.let { values ->
                            listOf("notificationMessageSoundChannelVersion", "notificationCallRingtoneChannelVersion").forEach { name ->
                                val key = intPreferencesKey(name)
                                values[key] = maxOf(values[key] ?: 0, previous.getValue("elementx_preferences")[key] ?: 0).let {
                                    require(it < Int.MAX_VALUE)
                                    it + 1
                                }
                            }
                        }
                        prepared.forEach { (name, values) -> store(name).edit {
                            it.removeImported(archive.unsetKeys[name].orEmpty())
                            it.mergeImported(values)
                        } }
                        val editor = sharedPreferences.edit()
                        archive.distributors.forEach { (key, value) -> editor.putString(key, value) }
                        check(editor.commit())
                        tweaks.prefetch()
                        if (Build.VERSION.SDK_INT >= 33 && archive.locale != null) {
                            require(archive.locale.length <= 1024 && archive.locale.matches(Regex("[a-zA-Z0-9,\\-]*")))
                            localeManager?.applicationLocales = LocaleList.forLanguageTags(archive.locale)
                        }
                    } catch (failure: Throwable) {
                        previous.forEach { (name, value) -> runCatching { store(name).updateData { value } } }
                        val editor = sharedPreferences.edit()
                        archive.distributors.keys.forEach { editor.remove(it) }
                        oldDistributors.forEach { (key, value) -> editor.putString(key, value) }
                        editor.commit()
                        files.forEach { it.delete() }
                        tweaks.prefetch()
                        if (Build.VERSION.SDK_INT >= 33 && previousLocale != null) {
                            runCatching { localeManager?.applicationLocales = previousLocale }
                        }
                        throw failure
                    }
                }
            }
        }
    }

    private suspend fun completeSnapshot(stores: MutableMap<String, Preferences>): Map<String, Set<String>> {
        val unset = mutableSetOf<String>()
        val app = stores.getValue("elementx_preferences").toMutablePreferences()
        fun <T : Any> capture(key: Preferences.Key<T>, value: T?) {
            if (key in app) return
            if (value == null) unset += key.name else app[key] = value
        }
        capture(booleanPreferencesKey("developerMode"), appPreferences.isDeveloperModeEnabledFlow().first())
        capture(booleanPreferencesKey("otherAccountsExpanded"), appPreferences.isOtherAccountsExpandedFlow().first())
        capture(booleanPreferencesKey("hideInviteAvatars"), appPreferences.getHideInviteAvatarsFlow().first())
        capture(stringPreferencesKey("theme"), appPreferences.getThemeFlow().first())
        capture(stringPreferencesKey("elementCallBaseUrl"), appPreferences.getCustomElementCallBaseUrlFlow().first())
        capture(intPreferencesKey("liveLocationMinimumDistanceUpdate"), appPreferences.getLiveLocationMinimumDistanceInMetersUpdateFlow().first())
        capture(stringPreferencesKey("logLevel"), appPreferences.getTracingLogLevelFlow().first().name)
        capture(stringPreferencesKey("traceLogPacks"), appPreferences.getTracingLogPacksFlow().first().joinToString(",") { it.key })
        capture(stringPreferencesKey("homeserverHistory"), appPreferences.getHomeserverHistoryFlow().first().joinToString("\n"))
        @Suppress("DEPRECATION")
        capture(stringPreferencesKey("timelineMediaPreviewValue"), appPreferences.getTimelineMediaPreviewValueFlow().first()?.name)
        capture(stringPreferencesKey("notificationMessageSoundUri"), appPreferences.getMessageSoundFlow().first().toStored())
        capture(stringPreferencesKey("notificationCallRingtoneUri"), appPreferences.getCallRingtoneFlow().first().toStored())
        capture(stringPreferencesKey("notificationMessageSoundDisplayName"), appPreferences.getMessageSoundDisplayNameFlow().first())
        capture(stringPreferencesKey("notificationCallRingtoneDisplayName"), appPreferences.getCallRingtoneDisplayNameFlow().first())
        stores["elementx_preferences"] = app.toPreferences()
        stores.keys.toList().forEach { name ->
            val values = stores.getValue(name).toMutablePreferences()
            fun <T : Any> default(key: Preferences.Key<T>, value: T) { if (key !in values) values[key] = value }
            when {
                SESSION_STORE.matches(name) -> {
                    listOf(
                        "sharePresence", "sendPublicReadReceipts", "renderReadReceipts",
                        "sendTypingNotifications", "renderTypingNotifications", "compressMedia",
                    ).forEach { default(booleanPreferencesKey(it), true) }
                    default(stringPreferencesKey("compressMediaPreset"), "STANDARD")
                }
                name.startsWith("push_store_") -> default(booleanPreferencesKey("notificationEnabled"), true)
                name == "elementx_rageshake" -> {
                    default(booleanPreferencesKey("enabled"), false)
                    default(floatPreferencesKey("sensitivity"), 0.5f)
                }
                name == "elementx_voice_player" -> default(intPreferencesKey("playback_speed_index"), 0)
                name == "vector_analytics" -> {
                    default(booleanPreferencesKey("user_consent"), false)
                    default(booleanPreferencesKey("did_ask_user_consent"), false)
                }
                name == "pin_code_store" -> default(booleanPreferencesKey("biometric_unlock_enabled"), false)
                name == "schildinext-appstate" -> default(stringPreferencesKey("SPACE_SELECTION"), "[]")
            }
            stores[name] = values.toPreferences()
        }
        return mapOf("elementx_preferences" to unset)
    }

    private fun validateAssetReferences(archive: SettingsArchive) {
        archive.assets.forEach { (name, bytes) ->
            require(ASSET_TARGETS.getValue(name).first in archive.stores)
            require(bytes.isNotEmpty() && bytes.size <= (if (name == "wallpaper") 50 else 10) * 1024 * 1024)
        }
        ASSET_TARGETS.forEach { (name, target) ->
            val reference = archive.stores[target.first]?.get(stringPreferencesKey(target.second)).orEmpty()
            if (reference.isNotEmpty() && reference !in setOf("silent", "element_default", "element_fade")) {
                require(name in archive.assets) { "Missing settings attachment" }
            }
        }
        archive.assets["wallpaper"]?.let { bytes ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            require(options.outWidth > 0 && options.outHeight > 0) { "Invalid wallpaper" }
        }
    }
}
