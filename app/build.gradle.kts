import java.util.Properties

plugins {
    // Kotlin support is built into AGP 9 — no separate kotlin-android plugin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing material, read from an untracked `keystore.properties` at the
 * repo root (or from the environment, for CI).
 *
 * Absent on a normal checkout, and that is the point: a developer who has never
 * been given the keystore still gets a working `assembleDebug`, and
 * `assembleRelease` falls back to unsigned rather than failing the configuration
 * phase for everyone. See the block in `android.signingConfigs` below.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.su.clubfair"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.su.clubfair"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Only the languages actually translated. Without this the APK carries
        // every locale AppCompat and Material ship strings for, and the app's
        // own resources silently fall back to English for all of them.
        resourceConfigurations += setOf("en", "th")
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("storeFile", "CLUBFAIR_STORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = signingValue("storePassword", "CLUBFAIR_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "CLUBFAIR_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "CLUBFAIR_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Side-by-side with a release build, so a tester can hold both and a
            // debug install never overwrites the one being demoed.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Null when no keystore has been supplied — the build then produces
            // an unsigned APK instead of failing.
            signingConfig = signingConfigs.getByName("release")
                .takeIf { it.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        // The About screen prints the version it is actually running.
        buildConfig = true
    }

    lint {
        // A lint error should stop a release, not be discovered in review.
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = true
        // Unused resources are worth knowing about — this repo was carrying
        // 2.3 MB of them.
        disable += setOf("GradleDependency")
        htmlReport = true
        sarifReport = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3)
    implementation(libs.backdrop)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
