import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(MyPlugins.kotlinMultiplatform)
    id(Plugins.Android.multiplatformLibrary)
}
kotlin {
    jvm("desktop")
    android {
        compileSdk = 37
        namespace = "com.abdownloadmanager.resources.contracts"
        minSdk = 33
    }
    sourceSets.commonMain.dependencies {
        implementation(libs.okio.okio)
    }
}

