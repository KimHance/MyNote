package com.hance.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    // AGP 9: buildFeatures 는 구체 extension 에 있으므로 타입을 좁혀 compose 를 켠다.
    when (commonExtension) {
        is ApplicationExtension -> commonExtension.buildFeatures.compose = true
        is LibraryExtension -> commonExtension.buildFeatures.compose = true
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))

        add("implementation", libs.findLibrary("androidx.activity.compose").get())
        add("implementation", libs.findLibrary("kotlinx.immutable").get())
        add("implementation", libs.findLibrary("androidx.compose.material3").get())
        add("implementation", libs.findLibrary("androidx.compose.ui").get())
        add("implementation", libs.findLibrary("androidx.compose.ui.tooling.preview").get())
        add("androidTestImplementation", libs.findLibrary("androidx.compose.ui.test").get())
        add("debugImplementation", libs.findLibrary("androidx.compose.ui.tooling").get())
        add("debugImplementation", libs.findLibrary("androidx.compose.ui.testManifest").get())

        // Slack compose-lints — Android Lint 경로의 Compose 설계 검사(ktlint 의 compose-rules 와 별개)
        add("lintChecks", libs.findLibrary("compose.lint.checks").get())
    }

    // 테마 토큰 전파는 CompositionLocal 의 정당한 용례다(MyNoteTheme.colors/textStyles/adaptive).
    // ktlint 쪽(compose:compositionlocal-allowlist)은 .editorconfig 에서 끈다.
    commonExtension.lint.disable += "ComposeCompositionLocalUsage"

    extensions.getByType<ComposeCompilerGradlePluginExtension>().apply {
        includeSourceInformation.set(true)
    }
}
