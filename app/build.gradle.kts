plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.personaroleplay.hrwdpx"
    minSdk = 24
    targetSdk = 36
    versionCode = 21
    versionName = "1.2.1beta"

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
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
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
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.ui)
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

tasks.register("copyApkToOutputs") {
  dependsOn("assembleDebug")
  val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
  val buildOutputsDir = layout.projectDirectory.dir("../.build-outputs")
  val apkDownloadDir = layout.projectDirectory.dir("../APK_DOWNLOAD")
  
  inputs.file(apkFile)
  outputs.dir(buildOutputsDir)
  outputs.dir(apkDownloadDir)
  
  doLast {
    val sourceFile = apkFile.get().asFile
    if (sourceFile.exists()) {
      val names = listOf(
        "DreamPlay-1.2.1beta.apk",
        "DreamPlay1.2.1beta.apk",
        "DreamPlay_1.2.1beta.apk",
        "DreamPlay-v1.2.1beta.apk",
        "DreamPlay_v1.2.1beta.apk",
        "DreamPlay.apk",
        "dreamplay-1.2.1beta.apk",
        "dreamplay1.2.1beta.apk",
        "dreamplay_1.2.1beta.apk",
        "dreamplay-v1.2.1beta.apk",
        "dreamplay_v1.2.1beta.apk",
        "dreamplay.apk",
        "DreamPlay_Roleplay_Pro-1.2.1beta.apk",
        "DreamPlayRoleplayPro-1.2.1beta.apk"
      )
      
      val destOutputsDir = buildOutputsDir.asFile
      destOutputsDir.mkdirs()
      names.forEach { name ->
        sourceFile.copyTo(File(destOutputsDir, name), overwrite = true)
      }
      
      val destDownloadDir = apkDownloadDir.asFile
      if (destDownloadDir.exists()) {
        destDownloadDir.listFiles()?.forEach { it.delete() }
      } else {
        destDownloadDir.mkdirs()
      }
      val singleApkName = "DreamPlay-1.2.1beta.apk"
      sourceFile.copyTo(File(destDownloadDir, singleApkName), overwrite = true)
      // Copy with updated name as well
      sourceFile.copyTo(File(destDownloadDir, "DreamPlay_Roleplay_Pro-1.2.1beta.apk"), overwrite = true)
      
      println("Successfully copied APK to destinations!")
    } else {
      throw GradleException("Built APK file not found at " + sourceFile.absolutePath)
    }
  }
}

afterEvaluate {
  tasks.findByName("assembleDebug")?.finalizedBy("copyApkToOutputs")
}

tasks.register("checkApkSizes") {
  val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
  val apkDownloadDir = layout.projectDirectory.dir("../APK_DOWNLOAD")
  val buildOutputsDir = layout.projectDirectory.dir("../.build-outputs")
  doLast {
    val apk = apkFile.get().asFile
    println("=== APK ORIGINAL SIZE ===")
    println("Path: ${apk.absolutePath}")
    println("Size: ${apk.length()} bytes")
    
    val downloadDir = apkDownloadDir.asFile
    println("=== APK_DOWNLOAD FILES ===")
    if (downloadDir.exists()) {
      downloadDir.listFiles()?.forEach { f ->
        println("File: ${f.name} - Size: ${f.length()} bytes")
      }
    } else {
      println("Folder not found.")
    }
    
    val outputsDir = buildOutputsDir.asFile
    println("=== .BUILD_OUTPUTS FILES ===")
    if (outputsDir.exists()) {
      outputsDir.listFiles()?.forEach { f ->
        if (f.name.endsWith(".apk")) {
          println("File: ${f.name} - Size: ${f.length()} bytes")
        }
      }
    } else {
      println("Folder not found.")
    }
  }
}
