import extension.setupDependencyInjection
import extension.testCommonDependencies

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "chat.matrice.features.trimmed"
}

setupDependencyInjection()

dependencies {
    implementation(projects.features.call.api)
    implementation(projects.features.callnative.api)
    implementation(projects.features.location.api)
    implementation(projects.features.poll.api)
    implementation(projects.features.enterprise.api)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.designsystem)
    implementation(projects.schildi.lib)
    implementation(libs.kotlinx.collections.immutable)
    testCommonDependencies(libs)
}
