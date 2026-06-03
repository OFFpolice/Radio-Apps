import java.io.File as JFile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.offpolice"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.webradio.vbot"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

val projectDirFile = layout.projectDirectory.asFile
val buildDirFile = layout.buildDirectory.get().asFile
val rootDirFile = layout.projectDirectory.asFile.parentFile

tasks.register("renameApkToWebRadioBot") {
  val bDir = buildDirFile
  val rDir = rootDirFile

  doLast {
    val debugApk = JFile(bDir, "outputs/apk/debug/app-debug.apk")
    val destDebugApk = JFile(bDir, "outputs/apk/debug/WebRadioBot.apk")
    if (debugApk.exists()) {
      debugApk.copyTo(destDebugApk, overwrite = true)
      println("Successfully renamed debug APK to WebRadioBot.apk")
    }
    val releaseApk = JFile(bDir, "outputs/apk/release/app-release-unsigned.apk")
    val destReleaseApk = JFile(bDir, "outputs/apk/release/WebRadioBot.apk")
    if (releaseApk.exists()) {
      releaseApk.copyTo(destReleaseApk, overwrite = true)
      println("Successfully renamed release APK to WebRadioBot.apk")
    }
    val releaseSignedApk = JFile(bDir, "outputs/apk/release/app-release.apk")
    if (releaseSignedApk.exists()) {
      releaseSignedApk.copyTo(destReleaseApk, overwrite = true)
      println("Successfully renamed signed release APK to WebRadioBot.apk")
    }

    val buildOutputsDir = JFile(rDir, ".build-outputs")
    if (buildOutputsDir.exists()) {
      val destFile = JFile(buildOutputsDir, "WebRadioBot.apk")
      if (debugApk.exists()) {
        debugApk.copyTo(destFile, overwrite = true)
        debugApk.copyTo(JFile(buildOutputsDir, "app-debug.apk"), overwrite = true)
      } else if (releaseSignedApk.exists()) {
        releaseSignedApk.copyTo(destFile, overwrite = true)
      } else if (releaseApk.exists()) {
        releaseApk.copyTo(destFile, overwrite = true)
      }
    }
  }
}

tasks.configureEach {
  if (name.startsWith("assemble")) {
    finalizedBy("renameApkToWebRadioBot")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("androidx.media3:media3-exoplayer:1.3.1")
  implementation("androidx.media3:media3-session:1.3.1")
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
