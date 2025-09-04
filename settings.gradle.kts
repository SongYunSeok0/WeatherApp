pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.12.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("com.google.devtools.ksp") version "2.0.21-1.0.27"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "weather"
include(":app")

// CITY_SEED_URL=https://gist.githubusercontent.com/SongYunSeok0/f55bc5ef05569e3746f8effee36257b7/raw/099b4a940dd6d193b656d1534f48e41f92e3987c/cities.json
// OWM_API_KEY=1dae27246742c2a0734dc5c4f62a5a5e