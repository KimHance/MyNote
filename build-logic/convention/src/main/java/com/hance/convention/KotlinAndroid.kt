package com.hance.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    // AGP 9: compileSdk 는 CommonExtension 에 남아있고, 나머지 블록은 구체 extension 으로 이동했다.
    commonExtension.compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()

    val minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
    when (commonExtension) {
        is ApplicationExtension -> commonExtension.configureSharedAndroid(minSdk)
        is LibraryExtension -> commonExtension.configureSharedAndroid(minSdk)
    }

    configureKotlin()

    dependencies {
        add("coreLibraryDesugaring", libs.findLibrary("android-desugarJdkLibs").get())
    }
}

private fun ApplicationExtension.configureSharedAndroid(minSdk: Int) {
    defaultConfig.minSdk = minSdk
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
}

private fun LibraryExtension.configureSharedAndroid(minSdk: Int) {
    defaultConfig.minSdk = minSdk
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
}

internal fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            val warningsAsErrors: String? by project
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())
            freeCompilerArgs.set(
                freeCompilerArgs.get() + listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                )
            )
        }
    }
}
