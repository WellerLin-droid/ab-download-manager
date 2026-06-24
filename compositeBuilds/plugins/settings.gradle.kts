dependencyResolutionManagement{
    versionCatalogs {
        create("libs"){
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}
include("installer-plugin")
include("common-android")
