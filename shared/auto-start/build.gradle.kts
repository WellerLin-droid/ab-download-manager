import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(MyPlugins.kotlinMultiplatform)
    id(Plugins.Android.multiplatformLibrary)
}
kotlin {
    jvm("desktop")
    android {
        compileSdk = 37
        namespace = "ir.amirab.util.startup"
        minSdk = 33
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:utils"))
        }
        val desktopMain by getting
        desktopMain.dependencies {
            //    // for windows, we use registry
            implementation(libs.jna.platform)
        }
    }
}


