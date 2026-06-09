import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
}

ksp {
    arg("room.generateKotlin", "true")
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LoopHabit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Material Icons (use lib catalog - compose extension not available in 1.9.0)
            implementation(libs.compose.icons.core)
            implementation(libs.compose.icons.extended)

            // Lifecycle (KMP)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.core)

            // Supabase (KMP)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.core)

            // Ktor (KMP)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Multiplatform Settings
            implementation(libs.multiplatformSettings)
            implementation(libs.multiplatform.settings.coroutines)

            // KotlinX (KMP)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            // Room (KMP - annotations available in commonMain since 2.6.0)
            implementation(libs.androidx.room.runtime)

            // SQLite Bundled (required for Room on non-Android platforms)
            implementation(libs.sqlite.bundled)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)

            // Android-specific Jetpack Compose (for Glance, etc.)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.material.icons.core)
            implementation(libs.androidx.compose.material.icons.extended)

            // Room Database
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.ktx)

            // DataStore
            implementation(libs.androidx.datastore.preferences)

            // Glance Widget
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)

            // WorkManager
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.work.rxjava3)

            // Ktor Android engine
            implementation(libs.ktor.client.okhttp)

            // Android-specific
            implementation(libs.androidx.documentfile)
            implementation(libs.gson)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.loophabit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.loophabit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase configuration (set via local.properties or environment)
        buildConfigField("String", "SUPABASE_URL", "\"https://your-project.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"your-anon-key\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
// Updated via script
