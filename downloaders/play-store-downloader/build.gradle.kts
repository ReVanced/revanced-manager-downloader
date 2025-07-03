plugins {
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.compose.compiler)
}

android {
    val packageName = "app.revanced.manager.plugin.downloader.play.store"
    namespace = packageName
    defaultConfig {
        applicationId = packageName
    }

    dependencies {
        implementation(libs.gplayapi)
        implementation(project(":arsclib"))
        implementation(project(":shared"))

        implementation(libs.ktor.core)
        implementation(libs.ktor.logging)
        implementation(libs.ktor.okhttp)

        implementation(libs.compose.activity)
        implementation(platform(libs.compose.bom))
        implementation(libs.compose.ui)
        implementation(libs.compose.ui.tooling)
        implementation(libs.compose.material3)
        implementation(libs.compose.webview)
    }

    buildFeatures {
        compose = true
        aidl = true
    }
}