pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("Amdroid/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Androidstudio"
include(":app")
project(":app").projectDir = file("Amdroid/app")
include(":reloj")
project(":reloj").projectDir = file("Amdroid/reloj")
