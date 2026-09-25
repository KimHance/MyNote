import java.util.Properties

plugins {
    alias(libs.plugins.hancenote.android.application)
    alias(libs.plugins.hancenote.android.application.compose)
    alias(libs.plugins.hancenote.android.hilt)
}

// local.properties 에서 노트 레포 좌표/토큰을 읽는다(토큰은 gitignore 됨).
// APK 내장은 임시 — 다음 작업에서 런타임 입력 + 암호화 저장으로 교체한다.
val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

fun prop(key: String, default: String): String = localProps.getProperty(key, default)

android {
    namespace = "com.hancekim.mynote"

    defaultConfig {
        applicationId = "com.hancekim.mynote"
        versionCode =
            libs.versions.appVersionCode
                .get()
                .toInt()
        versionName = libs.versions.appVersionName.get()

        buildConfigField("String", "GITHUB_OWNER", "\"${prop("github.owner", "KimHance")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${prop("github.repo", "MyNote")}\"")
        buildConfigField("String", "GITHUB_BRANCH", "\"${prop("github.branch", "main")}\"")
        // 토큰 기본값은 빈 문자열 → release 는 토큰 미내장(런타임 입력 전용).
        buildConfigField("String", "GITHUB_TOKEN", "\"\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // 디버그 빌드만 local.properties 토큰을 내장(개발 편의). release 는 빈 값 유지.
            buildConfigField("String", "GITHUB_TOKEN", "\"${prop("github.token", "")}\"")
        }
        release {
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
}
