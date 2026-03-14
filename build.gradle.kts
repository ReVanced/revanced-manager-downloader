import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    signing
}

subprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/registry")
            credentials(PasswordCredentials::class)
        }
    }
}

android {
    val packageName = "app.revanced.manager.downloaders"
    namespace = packageName
    defaultConfig {
        applicationId = packageName
    }

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        compileSdk = 36
        versionName = version.toString()
        //noinspection WrongGradleMethod
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

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters",
        )
    }
}

dependencies {
    "compileOnly"(libs.manager.api)

    implementation(libs.gplayapi)
    implementation(libs.arsclib)

    implementation(libs.ktor.core)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.okhttp)

    implementation(libs.fragment)
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