import java.util.Properties

plugins {
    alias(libs.plugins.hancenote.android.application)
    alias(libs.plugins.hancenote.android.application.compose)
    alias(libs.plugins.hancenote.android.hilt)
}

val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

fun prop(key: String): String = localProps.getProperty(key, "")

// 시드(owner · repo)가 없으면 토큰을 쓸 곳이 없으니 debug APK 에 넣지 않는다.
val seedToken = if (prop("github.owner").isBlank() || prop("github.repo").isBlank()) "" else prop("github.token")

val templateRepo =
    providers
        .gradleProperty("template.repo")
        .getOrElse("")
        .takeIf { Regex("[A-Za-z0-9._/-]*").matches(it) }
        .orEmpty()

// debug 에서만 -Ptitle.font=<이름> 으로 제목 글꼴 이름을 바꾼다(받지 못하는 경우를 보려고). release 는 늘 정식 이름이다.
val titleFontName =
    providers
        .gradleProperty("title.font")
        .getOrElse("")
        .takeIf { Regex("[A-Za-z0-9 ]*").matches(it) }
        .orEmpty()

android {
    namespace = "com.hancekim.mynote"

    defaultConfig {
        applicationId = "com.hancekim.mynote"
        versionCode =
            libs.versions.appVersionCode
                .get()
                .toInt()
        versionName = libs.versions.appVersionName.get()

        buildConfigField("String", "GITHUB_OWNER", "\"\"")
        buildConfigField("String", "GITHUB_REPO", "\"\"")
        buildConfigField("String", "GITHUB_BRANCH", "\"\"")
        buildConfigField("String", "GITHUB_TOKEN", "\"\"")
        buildConfigField("String", "TEMPLATE_REPO", "\"$templateRepo\"")
        buildConfigField("String", "TITLE_FONT_NAME", "\"\"")

        // litertlm 엔진(.so)이 CPU 종류마다 따로 들어 있어 기본값이면 안 쓰는 게
        // 25MB 실린다. 실기기·에뮬레이터 이미지가 전부 arm64 라 그것만 남긴다.
        // 인텔 맥에서 x86_64 에뮬레이터를 쓰게 되면 이 블록을 지운다.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "GITHUB_OWNER", "\"${prop("github.owner")}\"")
            buildConfigField("String", "GITHUB_REPO", "\"${prop("github.repo")}\"")
            buildConfigField("String", "GITHUB_BRANCH", "\"${prop("github.branch")}\"")
            buildConfigField("String", "GITHUB_TOKEN", "\"$seedToken\"")
            buildConfigField("String", "TITLE_FONT_NAME", "\"$titleFontName\"")
        }
        release {
            buildConfigField("String", "GITHUB_OWNER", "\"\"")
            buildConfigField("String", "GITHUB_REPO", "\"\"")
            buildConfigField("String", "GITHUB_BRANCH", "\"\"")
            buildConfigField("String", "GITHUB_TOKEN", "\"\"")
            buildConfigField("String", "TITLE_FONT_NAME", "\"\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designFoundation)
    implementation(projects.core.designSystem)
    implementation(projects.core.domain)
    implementation(projects.core.ai)
    implementation(projects.core.navigation)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.ui)
    implementation(projects.feature.notes)
    implementation(projects.feature.connect)
    implementation(projects.feature.settings)
    implementation(projects.feature.quiz)
    implementation(projects.feature.stats)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    // 셸(`shell/`) — Activity owner ViewModel 받기 · 상태 수집
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.litertlm.android)
}
