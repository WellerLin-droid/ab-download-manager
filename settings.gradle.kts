pluginManagement {
//    repositories {
//        maven("https://maven.aliyun.com/repository/public")
//        maven("https://maven.aliyun.com/repository/central")
//        maven("https://maven.aliyun.com/repository/google") // 对于Android项目很重要
//        maven("https://maven.aliyun.com/repository/gradle-plugin")
//        // 保留gradlePluginPortal()和google()，但将其放在国内源之后可能会减慢速度
//        google {
//            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")
//            }
//        }
//        mavenCentral()
//        gradlePluginPortal()
//    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
//        // 国内镜像优先
//        maven { url = uri("https://maven.aliyun.com/repository/public") }
//        maven("https://maven.aliyun.com/repository/google")
//        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { url = uri("https://maven.aliyun.com/repository/central") }
////        google {
////            url = uri("https://maven.aliyun.com/repository/google")
////            content {
////                includeGroupByRegex("com\\.android.*")
////                includeGroupByRegex("com\\.google.*")
////                includeGroupByRegex("androidx.*")
////            }
////        }
        // 官方源作为 fallback
        google()
        mavenCentral()
        // libxposed API 仓库（Snapshot 版本需要，Release 版本已在 Maven Central）
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "ABDownloadManager"

include("android:app")
include("desktop:app")
include("desktop:app-utils")
include("desktop:shared")
include("desktop:slf4j-impl")
include("desktop:mac_utils")
include("downloader:core")
include("downloader:monitor")
include("integration:server")
include("shared:utils")
include("shared:app")
include("shared:compose-utils")
include("shared:resources")
include("shared:resources:contracts")
include("shared:config")
include("shared:updater")
include("shared:auto-start")
include("shared:nanohttp4k")
includeBuild("./compositeBuilds/shared"){
    name="build-shared"
}
includeBuild("./compositeBuilds/plugins")
