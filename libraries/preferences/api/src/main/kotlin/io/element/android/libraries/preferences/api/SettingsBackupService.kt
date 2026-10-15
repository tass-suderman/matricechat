package io.element.android.libraries.preferences.api

import android.net.Uri

/** Portable local settings, independent of application name, package ID and Matrix sign-in credentials. */
interface SettingsBackupService {
    suspend fun exportSettings(destination: Uri): Result<Unit>
    suspend fun importSettings(source: Uri): Result<Unit>
}
