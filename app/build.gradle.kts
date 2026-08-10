import java.util.Properties

plugins {
    // Kotlin support is built into AGP 9 — no separate kotlin-android plugin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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

        // The **web** OAuth client id, even on Android: it becomes the ID token's
        // `aud`, which is what su-server compares against its own
        // GOOGLE_CLIENT_ID. Supply it with
        //   ./gradlew installDebug -PgoogleWebClientId=...apps.googleusercontent.com
        // or in ~/.gradle/gradle.properties so it is never committed.
        //
        // Empty by default, and the app keeps the Google button disabled rather
        // than opening a sheet that cannot succeed — see GoogleSignIn.isConfigured.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${properties["googleWebClientId"] ?: ""}\"",
        )

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

            // localhost, reached over an `adb reverse` tunnel — `make reverse`,
            // which `make install` runs for you.
            //
            // Not 10.0.2.2, the emulator's usual alias for the host: su-server's
            // compose file publishes on `127.0.0.1:8080` deliberately, so the API
            // is never exposed on a public interface, and that alias does not
            // reach a loopback-only bind. `adb reverse` tunnels the device's own
            // localhost to the host over adb, which needs no firewall change and
            // leaves that binding alone.
            //
            // A physical phone on the same network needs the laptop's LAN address
            // instead, and su-server bound wider to accept it:
            //   -PclubfairApiBase=http://192.168.1.x:8080
            // Passed on the command line rather than edited here, so a personal
            // address is never committed.
            //
            // Cleartext is permitted for this variant only; see
            // src/debug/res/xml/network_security_config.xml.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${properties["clubfairApiBase"] ?: "http://localhost:8080"}\"",
            )
        }
        release {
            // No default. A release build pointed at someone's laptop would be
            // worse than one that fails to configure, so this must be supplied:
            //   ./gradlew assembleRelease -PclubfairApiBase=https://api.example.ac.th
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${properties["clubfairApiBase"] ?: ""}\"",
            )
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

        // java.time on API 24.
        //
        // su-server sends RFC 3339 timestamps and the app parses them with
        // `Instant`, which arrived in API 26 — this app's floor is 24, so without
        // desugaring that is a crash on Android 7 rather than a compile error.
        // Lint found it; a user would have found it otherwise.
        //
        // The alternative was hand-rolling RFC 3339 with SimpleDateFormat, across
        // fractional seconds and offsets. That is a bug farm for the sake of one
        // dependency.
        isCoreLibraryDesugaringEnabled = true
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

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.okhttp.logging)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

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
