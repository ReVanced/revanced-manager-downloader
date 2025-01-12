plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    publishing
    signing
}

dependencies {
    compileOnly(libs.plugin.api)
}

android {
    val packageName = "app.revanced.manager.plugin.downloader.example"

    namespace = packageName
    compileSdk = 35

    defaultConfig {
        applicationId = packageName
        minSdk = 26
        targetSdk = 35
        versionName = version.toString()
        versionCode = versionName!!.filter { it.isDigit() }.toInt()
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            val keystoreFile = file("keystore.jks")
            signingConfig =
                if (keystoreFile.exists()) {
                    signingConfigs.create("release") {
                        storeFile = keystoreFile
                        storePassword = System.getenv("KEYSTORE_PASSWORD")
                        keyAlias = System.getenv("KEYSTORE_ENTRY_ALIAS")
                        keyPassword = System.getenv("KEYSTORE_ENTRY_PASSWORD")
                    }
                } else {
                    signingConfigs["debug"]
                }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    applicationVariants.all {
        outputs.all {
            this as com.android.build.gradle.internal.api.ApkVariantOutputImpl

            outputFileName = "${rootProject.name}-$version.apk"
        }
    }
}

tasks {
    val assembleReleaseSignApk by registering {
        dependsOn("assembleRelease")

        val apk = layout.buildDirectory.file("outputs/apk/release/${rootProject.name}-$version.apk")

        inputs.file(apk).withPropertyName("input")
        outputs.file(apk.map { it.asFile.resolveSibling("${it.asFile.name}.asc") })

        doLast {
            signing {
                useGpgCmd()
                sign(*inputs.files.files.toTypedArray())
            }
        }
    }

    // Used by gradle-semantic-release-plugin.
    // Tracking: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435.
    publish {
        dependsOn(assembleReleaseSignApk)
    }
}
