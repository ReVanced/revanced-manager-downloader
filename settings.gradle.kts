rootProject.name = "revanced-manager-downloaders"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").getOrElse(System.getenv("GITHUB_ACTOR"))
                password = providers.gradleProperty("gpr.key").getOrElse(System.getenv("GITHUB_TOKEN"))
            }
        }
    }
}

include(":shared")
file("downloaders").listFiles()
    ?.forEach {
        include(":downloaders:${it.name}")
        project(":downloaders:${it.name}").projectDir = file("downloaders/${it.name}")
    }
