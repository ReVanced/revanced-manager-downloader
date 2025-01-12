rootProject.name = "revanced-manager-downloader-template"

pluginManagement.repositories {
    gradlePluginPortal()
    google()
}

dependencyResolutionManagement.repositories {
    mavenCentral()
    google()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/revanced/registry")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
