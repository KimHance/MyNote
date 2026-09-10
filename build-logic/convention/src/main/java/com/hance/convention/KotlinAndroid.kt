package com.hance.convention

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

    // AGP 9 에서 블록 메서드(defaultConfig { })는 구체 extension 으로 옮겨갔지만,
    // getter 는 CommonExtension 에 남아있어 Application/Library 를 한 번에 설정할 수 있다.
    commonExtension.defaultConfig.minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    commonExtension.testOptions.unitTests.isIncludeAndroidResources = true

    // Gradle 9 부터 테스트를 하나도 못 찾으면 test 태스크가 실패한다.
    // 아직 테스트가 없는 모듈이 있어 프로젝트 전체 `test` 실행을 막으므로 끈다.
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
            // `by project` 위임은 Gradle 10 에서 제거된다. provider 는 configuration cache 에도 안전하다.
            allWarningsAsErrors.set(
                providers.gradleProperty("warningsAsErrors").orNull.toBoolean(),
            )
        }
    }
}
