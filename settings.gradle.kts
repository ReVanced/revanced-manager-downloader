rootProject.name = "revanced-manager-downloaders"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials(PasswordCredentials::class)
        }
    }
}

include(":shared")
include(":arsclib")
file("downloaders").listFiles()
    ?.forEach {
        include(":downloaders:${it.name}")
        project(":downloaders:${it.name}").projectDir = file("downloaders/${it.name}")
    }
