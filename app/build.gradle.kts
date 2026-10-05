import java.util.Properties

plugins {
    alias(libs.plugins.hancenote.android.application)
    alias(libs.plugins.hancenote.android.application.compose)
    alias(libs.plugins.hancenote.android.hilt)
}

// debug 빌드만 local.properties 의 github.owner/repo/branch/token 을 "첫 실행 시드" 로 내장한다
// (개발 편의 · RC-19). release 는 값이 있어도 항상 빈 값 — 저장소 연결은 앱 화면에서 한다.
val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

fun prop(key: String): String = localProps.getProperty(key, "")

android {
    namespace = "com.hancekim.mynote"

    defaultConfig {
        applicationId = "com.hancekim.mynote"
        versionCode =
            libs.versions.appVersionCode
                .get()
                .toInt()
        versionName = libs.versions.appVersionName.get()

        // 코드에 기본 저장소를 두지 않는다 — 값은 아래 buildTypes 가 정한다.
        buildConfigField("String", "GITHUB_OWNER", "\"\"")
        buildConfigField("String", "GITHUB_REPO", "\"\"")
        buildConfigField("String", "GITHUB_BRANCH", "\"\"")
        buildConfigField("String", "GITHUB_TOKEN", "\"\"")

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
            buildConfigField("String", "GITHUB_TOKEN", "\"${prop("github.token")}\"")
        }
        release {
            // local.properties 에 값이 있어도 공개 빌드에는 시드 · 토큰이 없다(결정 22).
            // defaultConfig 가 나중에 바뀌어도 새지 않도록 여기서도 못 박는다.
            buildConfigField("String", "GITHUB_OWNER", "\"\"")
            buildConfigField("String", "GITHUB_REPO", "\"\"")
            buildConfigField("String", "GITHUB_BRANCH", "\"\"")
            buildConfigField("String", "GITHUB_TOKEN", "\"\"")
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
    implementation(projects.core.designFoundation)
    implementation(projects.core.designSystem)
    implementation(projects.core.domain)
    implementation(projects.core.ai)
    implementation(projects.core.navigation)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.feature.notes)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.litertlm.android)
}
