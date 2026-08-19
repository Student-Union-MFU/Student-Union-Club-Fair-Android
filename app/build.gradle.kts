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

/**
 * An untracked `.env` at the repo root, for local configuration.
 *
 * Read at configure time so a value can come from the file rather than from a
 * `-P` flag on every command. `.env` is gitignored — a client id is public by
 * design, unlike a client *secret*, but it is still per-developer and
 * per-environment and has no business in the repo.
 */
val dotenv = Properties().apply {
    val file = rootProject.file(".env")
    if (file.exists()) file.inputStream().use(::load)
}

/**
 * One local setting, from the first place that has it.
 *
 * Gradle property first — `-PgoogleWebClientId=…` or `~/.gradle/gradle.properties`
 * — so a command-line override always wins over a file someone forgot they wrote.
 * Then `.env`, then the environment, then a default.
 */
fun localConfig(gradleProperty: String, envKey: String, default: String = ""): String =
    ((properties[gradleProperty] as String?)
        ?: dotenv.getProperty(envKey)
        ?: System.getenv(envKey)
        ?: default)
        .let(::unquote)

/**
 * Strips one matching pair of surrounding quotes.
 *
 * `.env` files are conventionally written either way — `KEY=value` and
 * `KEY="value"` mean the same thing to a shell — but [Properties] is not a shell
 * and keeps the quotes as part of the value. Every value here ends up inside a
 * generated `String` literal, so a stray quote is not a wrong client id, it is a
 * `BuildConfig.java` that will not compile.
 */
fun unquote(raw: String): String {
    val value = raw.trim()
    val quoted = value.length >= 2 &&
        (value.startsWith('"') && value.endsWith('"') ||
            value.startsWith('\'') && value.endsWith('\''))
    return if (quoted) value.substring(1, value.length - 1) else value
}

android {
    namespace = "com.su.clubfair"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.su.clubfair"
        minSdk = 24
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The OAuth client id Credential Manager requests a token for.
        //
        // ⚠ It must be the **Web** client id, not the Android one, even though
        // this is an Android app. Credential Manager's `setServerClientId` is
        // documented as taking the server's client id, and the token it returns
        // carries that value as its `aud` — which is exactly what su-server
        // compares against its own GOOGLE_CLIENT_ID. An Android client id is
        // never pasted anywhere: it exists in the console only so Google will
        // issue a token to this package and signing certificate at all.
        //
        // From, in order: -PgoogleWebClientId, the root .env's
        // GOOGLE_OAUTH_CLIENT_ID_SERVER, or the environment. The key says
        // _SERVER because that is whose id it is — su-server's — and the file
        // also carries the Android one, which nothing reads. Empty by default,
        // and the app then keeps the Google button disabled rather than opening
        // a sheet that cannot succeed — see GoogleSignIn.isConfigured.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localConfig("googleWebClientId", "GOOGLE_OAUTH_CLIENT_ID_SERVER")}\"",
        )

        // Which intakes may sign up, as the leading digits of a student id —
        // "69" is the 2569 intake, so 66-69 is the four years currently on
        // campus. Mirrors su-server's own default and its
        // CLUBFAIR_INTAKE_PREFIXES so the form can say no before a round trip;
        // the server remains the one that decides, and is the only side that
        // knows whether an account already exists. Comma-separated for a fair
        // run for two intakes, "*" to accept any.
        //
        // Configured rather than compiled in because the answer changes every
        // year, and a Gradle property is cheaper than a Play Store release.
        buildConfigField(
            "String",
            "INTAKE_PREFIXES",
            "\"${localConfig("clubfairIntakePrefixes", "CLUBFAIR_INTAKE_PREFIXES", "66,67,68,69")}\"",
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
            // address is never committed. Set CLUBFAIR_API_BASE_DEBUG in `.env`
            // instead if that address is where every debug build should go.
            //
            // Cleartext is permitted for this variant only; see
            // src/debug/res/xml/network_security_config.xml.
            //
            // ---------------------------------------------------------------
            // This deliberately does NOT read `CLUBFAIR_API_BASE`, which is the
            // release target and is set to production in `.env`.
            //
            // It used to, and the whole point of the localhost default was lost:
            // `.env` is read ahead of the default, so every debug build silently
            // pointed at api.studentunion.social. A prize threshold was changed
            // in the local database, and the app kept showing the old one through
            // a clean, a reinstall and a pull-to-refresh — because it had never
            // once asked that database. The two build types now read two
            // different keys, so nothing set for production can be inherited by
            // a debug build by accident.
            //
            // Switching a debug build between servers signs you out, and that is
            // correct rather than a bug: a session token from one server is not
            // valid at the other, the first refresh gets a 401, and the app ends
            // the session. Expect to log in again after changing this.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localConfig("clubfairApiBase", "CLUBFAIR_API_BASE_DEBUG", "http://localhost:8080")}\"",
            )
        }
        release {
            // Production, from `CLUBFAIR_API_BASE` in `.env` — the key the debug
            // build above pointedly does not read.
            //
            // No default. A release build pointed at someone's laptop would be
            // worse than one that fails to configure, so this must be supplied:
            //   ./gradlew assembleRelease -PclubfairApiBase=https://api.example.ac.th
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localConfig("clubfairApiBase", "CLUBFAIR_API_BASE")}\"",
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

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

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
