plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ktlint)
    `java-library`
}
kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        optIn.add("kotlin.time.ExperimentalTime")

        extraWarnings = true
        allWarningsAsErrors = true
        progressiveMode = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
