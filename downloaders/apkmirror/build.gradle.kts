plugins {
    alias(libs.plugins.kotlin.parcelize)
}

android {
    val packageName = "app.revanced.manager.plugin.downloader.apkmirror"
    namespace = packageName
    defaultConfig {
        applicationId = packageName
    }

    dependencies {
        implementation(project(":shared"))
    }
}