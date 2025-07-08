plugins {
   alias(libs.plugins.android.library)
   alias(libs.plugins.kotlin.android)
}

android {
   namespace = "app.revanced.manager.plugin.shared"
   compileSdk = 35

   defaultConfig {
      minSdk = 26
   }

   compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
   }

   kotlinOptions {
      jvmTarget = "17"
   }
}

dependencies {
   implementation(libs.kotlinx.coroutines.core)
   api(project(":arsclib"))
}