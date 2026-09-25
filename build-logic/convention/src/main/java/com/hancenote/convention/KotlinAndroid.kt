package com.hancenote.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    commonExtension.defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    commonExtension.testOptions.unitTests.isIncludeAndroidResources = true

    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }

    configureKotlin()

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("android-desugarJdkLibs").get())
    }
}

internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(
                providers.gradleProperty("warningsAsErrors").orNull.toBoolean(),
            )
        }
    }
}
