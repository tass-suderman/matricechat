import extension.testCommonDependencies
import extension.setupDependencyInjection

plugins {
    id("io.element.android-compose-library")
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "chat.schildi.matrixsdk"
}

setupDependencyInjection()

dependencies {
    implementation(projects.libraries.di)
    implementation(libs.serialization.json)
    implementation(platform(libs.network.okhttp.bom))
    implementation(libs.network.okhttp)
    testCommonDependencies(libs)
    implementation(libs.androidx.emoji2)
    implementation(project(":libraries:matrix:api"))
    implementation(project(":annotations"))
}
