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
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://s01.oss.sonatype.org/content/groups/public/")
        maven("https://jitpack.io")
    }
}

rootProject.name = "APPSAS"
include(":app")