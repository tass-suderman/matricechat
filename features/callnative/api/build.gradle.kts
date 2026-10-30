/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import extension.isMatriceTrimmed

plugins {
    // NativeCallHost renders the call, so this module carries a composable.
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.callnative.api"
}

if (!isMatriceTrimmed) {
    extensions.configure<com.android.build.api.dsl.LibraryExtension> {
        sourceSets.getByName("main").kotlin.directories.add("src/full/kotlin")
    }
}

// Temporary: `-PelementCallLocalVersion` points this at a locally published build of
// element-call-android. Goes with the repositories in settings.gradle.kts.
val elementCallVersion: String = providers.gradleProperty("elementCallLocalVersion")
    .getOrElse(libs.versions.element.call.get())

dependencies {
    // api, not implementation: ElementCallMatrixTransport is part of ElementCallTransportFactory's
    // signature, and CallData of NativeCallEntryPoint's.
    // api, so that consumers of this module inherit the versions the BOM pins - the artifact
    // below is declared without one.
    if (!isMatriceTrimmed) {
        api(platform("io.element.android:element-call-bom:$elementCallVersion"))
        api(libs.element.call.api)
    }
    api(projects.features.call.api)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.matrix.api)
}
