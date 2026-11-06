import java.util.Base64
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  
  alias(libs.plugins.google.services)
  alias(libs.plugins.google.crashlytics)
  // alias(libs.plugins.google.perf)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


val debugKeystoreFile = file("${rootDir}/debug.keystore")
if (!debugKeystoreFile.exists()) {
    val base64File = file("${rootDir}/debug.keystore.base64")
    if (base64File.exists()) {
        val keystoreBase64 = base64File.readText().trim().replace("\\s".toRegex(), "")
        if (keystoreBase64.isNotEmpty()) {
            try {
                debugKeystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
            } catch (e: Exception) {
                project.logger.error("Failed to decode keystore base64", e)
            }
        }
    }
}

val detectedStoreType = run {
    if (debugKeystoreFile.exists() && debugKeystoreFile.length() > 4) {
        val bytes = ByteArray(4)
        debugKeystoreFile.inputStream().use { it.read(bytes) }
        if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xED.toByte() && bytes[2] == 0xFE.toByte() && bytes[3] == 0xED.toByte()) {
            "jks"
        } else {
            "pkcs12"
        }
    } else {
        "pkcs12"
    }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  val customCode = project.findProperty("customVersionCode")?.toString()?.toIntOrNull()
  val customName = project.findProperty("customVersionName")?.toString()

  defaultConfig {
    applicationId = "com.aistudio.weeklyfinance.jeymem"
    minSdk = 26
    targetSdk = 36
    versionCode = customCode ?: 100
    versionName = customName ?: "10.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  val signingKeyAlias = System.getenv("RELEASE_KEY_ALIAS")?.takeIf { it.isNotBlank() }
      ?: System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
      ?: System.getenv("SIGNING_KEY_ALIAS")?.takeIf { it.isNotBlank() }
      ?: project.findProperty("RELEASE_KEY_ALIAS")?.toString()?.takeIf { it.isNotBlank() }
      ?: project.findProperty("KEY_ALIAS")?.toString()?.takeIf { it.isNotBlank() }
      ?: "androiddebugkey"

  val signingKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: System.getenv("SIGNING_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: project.findProperty("RELEASE_KEY_PASSWORD")?.toString()?.takeIf { it.isNotBlank() }
      ?: project.findProperty("KEY_PASSWORD")?.toString()?.takeIf { it.isNotBlank() }
      ?: "android"

  val signingStorePassword = System.getenv("RELEASE_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: System.getenv("STORE_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: System.getenv("SIGNING_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
      ?: project.findProperty("RELEASE_STORE_PASSWORD")?.toString()?.takeIf { it.isNotBlank() }
      ?: project.findProperty("STORE_PASSWORD")?.toString()?.takeIf { it.isNotBlank() }
      ?: "android"

  val signingStoreType = System.getenv("RELEASE_STORE_TYPE")?.takeIf { it.isNotBlank() }
      ?: System.getenv("STORE_TYPE")?.takeIf { it.isNotBlank() }
      ?: System.getenv("SIGNING_STORE_TYPE")?.takeIf { it.isNotBlank() }
      ?: project.findProperty("RELEASE_STORE_TYPE")?.toString()?.takeIf { it.isNotBlank() }
      ?: project.findProperty("STORE_TYPE")?.toString()?.takeIf { it.isNotBlank() }
      ?: detectedStoreType

  signingConfigs {
    create("release") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = signingStorePassword
      keyAlias = signingKeyAlias
      keyPassword = signingKeyPassword
      storeType = signingStoreType
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = signingStorePassword
      keyAlias = signingKeyAlias
      keyPassword = signingKeyPassword
      storeType = signingStoreType
    }
  }

  lint {
    abortOnError = false
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

// Ensure .env and .env.example exist so Secrets Gradle Plugin doesn't fail the build
val envFile = file("${rootDir}/.env")
if (!envFile.exists()) {
    envFile.writeText("GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE\n")
}
val envExampleFile = file("${rootDir}/.env.example")
if (!envExampleFile.exists()) {
    envExampleFile.writeText("GEMINI_API_KEY=YOUR_GEMINI_API_KEY_HERE\n")
}

// Ensure google-services.json exists so Google Services plugin doesn't fail on CI/release builds if missing
val googleServicesFile = file("${projectDir}/google-services.json")
if (!googleServicesFile.exists()) {
    googleServicesFile.writeText("""
{
  "project_info": {
    "project_number": "455008013486",
    "firebase_url": "https://collection-app-2007-default-rtdb.asia-southeast1.firebasedatabase.app",
    "project_id": "collection-app-2007",
    "storage_bucket": "collection-app-2007.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:455008013486:android:531e28ee052201e9a5f13f",
        "android_client_info": {
          "package_name": "com.aistudio.weeklyfinance.khcrwt"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyBAuCr6O-bKtLFqUQag6N_DLTKP16NF5RQ"
        }
      ],
      "services": {}
    }
  ],
  "configuration_version": "1"
}
    """.trimIndent())
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)
  // implementation(libs.firebase.perf)
  implementation(libs.firebase.appcheck)
  implementation("com.google.firebase:firebase-database")
  implementation("com.google.firebase:firebase-storage")
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-inappmessaging-display")
  implementation("com.google.firebase:firebase-messaging")
  implementation("com.google.firebase:firebase-config")
  implementation("androidx.credentials:credentials:1.2.2")
  implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("net.zetetic:android-database-sqlcipher:4.5.4")
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.auth)
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
