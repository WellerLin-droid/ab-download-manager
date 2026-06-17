import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(MyPlugins.kotlinMultiplatform)
    id(Plugins.Android.multiplatformLibrary)
    id(Plugins.Kotlin.serialization)
}
kotlin {
    jvm("desktop")
    android {
        compileSdk = 36
        namespace = "com.abdownloadmanager.updater"
        minSdk = 26
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.serialization.json)
            api(libs.okhttp.okhttp)
            api(libs.kotlin.coroutines.core)
            implementation(project(":shared:utils"))
            implementation(libs.semver)
            implementation("ir.amirab.util:platform:1")
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.jna.platform)
        }
    }
}

