import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.room)
}

val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Release signing: keystore.properties at the project root (local) or SIGNING_* environment variables (CI).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

// Monetization (AdMob + "remove ads" purchase). Off by default; enable per app with ads.enabled=true in
// gradle.properties. Google's test IDs are used everywhere except prodRelease, which reads the real ones.
val adsEnabled = providers.gradleProperty("ads.enabled").orNull?.toBoolean() ?: false
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
fun adMobValue(key: String, fallback: String): String = providers.gradleProperty(key).orNull ?: fallback

// AI: requests go to your Cloudflare Worker (see README "AI"). Set ai.workerUrl in gradle.properties.
val aiWorkerUrl = providers.gradleProperty("ai.workerUrl").orNull ?: "https://ai.example.com/"

android {
    namespace = "com.viettel.appbase"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.viettel.appbase"
        minSdk = 24
        targetSdk = 36
        // CI overrides with -PversionCode=<build number> -PversionName=<tag>.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("versionName").orNull ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("Boolean", "FIREBASE_CONFIGURED", hasGoogleServices.toString())
        // Store listing id: without the .debug suffix, so "Rate app" always opens the real listing.
        buildConfigField("String", "PLAY_STORE_PACKAGE", "\"$applicationId\"")

        buildConfigField("Boolean", "ADS_ENABLED", adsEnabled.toString())
        buildConfigField("String", "AI_BASE_URL", "\"$aiWorkerUrl\"")
        buildConfigField(
            "String",
            "REMOVE_ADS_PRODUCT_ID",
            "\"${adMobValue("billing.removeAdsProductId", "remove_ads")}\"",
        )
        // The AdMob SDK crashes at startup without an app id, so one is always set (test id unless prodRelease).
        manifestPlaceholders["adMobAppId"] = testAdMobAppId
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        // Local demo auth, no backend or Firebase needed.
        create("mock") {
            dimension = "environment"
            buildConfigField("Boolean", "USE_FAKE_AUTH", "true")
            buildConfigField("Boolean", "USE_FAKE_AI", "true")
            buildConfigField("String", "BASE_URL", "\"https://dev.example.com/api/\"")
        }
        create("dev") {
            dimension = "environment"
            buildConfigField("Boolean", "USE_FAKE_AUTH", "false")
            buildConfigField("Boolean", "USE_FAKE_AI", "false")
            buildConfigField("String", "BASE_URL", "\"https://dev.example.com/api/\"")
            buildConfigField(
                "String",
                "AI_BASE_URL",
                "\"${providers.gradleProperty("ai.workerUrl.dev").orNull ?: aiWorkerUrl}\"",
            )
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("Boolean", "USE_FAKE_AUTH", "false")
            buildConfigField("Boolean", "USE_FAKE_AI", "false")
            buildConfigField("String", "BASE_URL", "\"https://example.com/api/\"")
            buildConfigField(
                "String",
                "AI_BASE_URL",
                "\"${providers.gradleProperty("ai.workerUrl.prod").orNull ?: aiWorkerUrl}\"",
            )
            manifestPlaceholders["adMobAppId"] = adMobValue("admob.appId", testAdMobAppId)
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${adMobValue("admob.bannerId", testBannerId)}\"")
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_ID",
                "\"${adMobValue("admob.interstitialId", testInterstitialId)}\"",
            )
        }
    }

    signingConfigs {
        val storeFilePath = signingValue("storeFile", "SIGNING_STORE_FILE")
        if (storeFilePath != null) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Never serve real ads to debug builds: clicks on your own ads can get the AdMob account banned.
            manifestPlaceholders["adMobAppId"] = testAdMobAppId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testBannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric sets FileDescriptor internals through jdk.internal.access on JDK 17+.
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
    sourceSets {
        // Robolectric reads the tested variant's assets: expose Room schemas to debug only (not shipped in release).
        getByName("debug").assets.directories.add("$projectDir/schemas")
    }
    lint {
        warningsAsErrors = true
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi",
        )
    }
}

androidComponents {
    // Demo auth must never ship to the Play Store.
    beforeVariants(selector().withBuildType("release").withFlavor("environment" to "mock")) { variant ->
        variant.enable = false
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// Formatting + Compose rules. Auto-fix with ./gradlew ktlintFormat; rules live in .editorconfig.
ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

// Code smells (complexity, exception handling, naming). Rules: config/detekt/detekt.yml.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/debug/java", "src/release/java", "src/test/java", "src/androidTest/java")
}

// detekt 1.23 embeds its own Kotlin compiler; keep that version instead of the project's.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}

dependencies {
    // BOMs: pin versions for the Compose and Firebase artifacts below
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))

    // AndroidX core: KTX extensions, system splash screen, EXIF (photo orientation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)

    // Jetpack Compose UI + Material 3
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation (type-safe routes)
    implementation(libs.androidx.navigation.compose)

    // Dependency injection (Koin)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Networking: Retrofit + OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    // Serialization (JSON for APIs, ProtoBuf for DataStore) + coroutines await() for Play/Firebase tasks
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    // Local storage: DataStore (preferences) + Room (database)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Firebase: analytics, auth, crash reporting, push, App Check (Play Integrity; debug provider in debug builds)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    // Google Play: in-app review + in-app update
    implementation(libs.play.app.update)
    implementation(libs.play.review)

    // Monetization: AdMob ads, UMP consent, Play Billing ("remove ads"); off unless ads.enabled=true
    implementation(libs.admob)
    implementation(libs.billing)
    implementation(libs.ump)

    // Image loading (Coil 3)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Logging
    implementation(libs.timber)

    // Code quality: Compose rules for ktlint
    ktlintRuleset(libs.compose.rules.ktlint)

    // Unit tests (JVM + Robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)

    // Instrumented tests (device)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug-only tooling: Compose previews/inspector, test manifest
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
