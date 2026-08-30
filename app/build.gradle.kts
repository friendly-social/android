import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.konan.properties.propertyString

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "friendly.android"

    defaultConfig {
        applicationId = "friendly.android"
        minSdk = 29
        targetSdk = 37
        compileSdk = 37
        versionCode = 3
        versionName = "1.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.jks")

            val localProperties =
                gradleLocalProperties(rootProject.projectDir, providers)

            storePassword = localProperties.propertyString("keystorePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
            keyAlias = localProperties.propertyString("keystoreKeyAlias")
                ?: System.getenv("KEYSTORE_KEY_ALIAS")
            keyPassword = localProperties.propertyString("keystoreKeyPassword")
                ?: System.getenv("KEYSTORE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add(
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

dependencies {
    // Will use alpha version till the material3-expressive release
    implementation(platform(libs.firebase.bom))
    implementation(platform(libs.androidx.compose.bom.alpha))
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.molecule)
    implementation(libs.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor)
    implementation(libs.androidx.exifinterface)
    implementation(libs.zxing.core)
    implementation(libs.alexzhirkevich.qrose)
    implementation(libs.ktor.logging)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.datetime)

    implementation(libs.friendly.sdk)
    implementation(projects.cards)
    implementation(projects.markdowntext)

    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
}
