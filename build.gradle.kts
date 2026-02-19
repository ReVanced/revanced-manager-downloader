import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
    signing
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/revanced/registry")
        credentials {
            username = providers.gradleProperty("gpr.user")
                .getOrElse(System.getenv("GITHUB_ACTOR"))
            password =
                providers.gradleProperty("gpr.key").getOrElse(System.getenv("GITHUB_TOKEN"))
        }
    }
}
android {
    val packageName = "app.revanced.manager.downloaders"
    namespace = packageName
    defaultConfig {
        applicationId = packageName
    }

    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    "compileOnly"(rootProject.libs.manager.api)

    implementation(libs.gplayapi)
    implementation(libs.arsclib)

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

configure<AppExtension> {
    compileSdkVersion(35)

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionName = version.toString()
        versionCode = versionName!!.filter { it.isDigit() }.toInt()
    }

    buildTypes {
        getByName("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val keystoreFile = file("${rootDir}/keystore.jks")
            signingConfig =
                if (keystoreFile.exists()) {
                    signingConfigs.create("release") {
                        storeFile = keystoreFile
                        storePassword = System.getenv("KEYSTORE_PASSWORD")
                        keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                        keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                    }
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    applicationVariants.all {
        outputs.all {
            this as ApkVariantOutputImpl
            outputFileName = "revanced-manager-downloaders-$version.apk"
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.register("assembleReleaseSignApk") {
    dependsOn("assembleRelease")

    val apk =
        layout.buildDirectory.file("outputs/apk/release/revanced-manager-downloaders-$version.apk")

    inputs.file(apk).withPropertyName("input")
    outputs.file(apk.map { it.asFile.resolveSibling("${it.asFile.name}.asc") })

    doLast {
        project.configure<SigningExtension> {
            useGpgCmd()
            sign(*inputs.files.files.toTypedArray())
        }
    }
}

tasks.named("publish") {
    dependsOn("assembleReleaseSignApk")
}