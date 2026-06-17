package myPlugins

fun getOptIns(): Set<String> = setOf(
    "androidx.compose.animation.ExperimentalAnimationApi",
    "androidx.compose.foundation.ExperimentalFoundationApi",
    "androidx.compose.ui.ExperimentalComposeUiApi",
)

fun getFeatures(): Set<String> = setOf(
    "context-parameters",
)

val jvmToolchainVersion =  providers.gradleProperty("jvm.toolchain").get().toInt()
