import java.util.Properties

plugins {
    alias(libs.plugins.billboard.android.application)
    alias(libs.plugins.billboard.android.application.compose)
    alias(libs.plugins.billboard.android.hilt)
}

// local.properties 에서 노트 레포 좌표/토큰을 읽는다(토큰은 gitignore 됨).
// APK 내장은 임시 — 다음 작업에서 런타임 입력 + 암호화 저장으로 교체한다.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(key: String, default: String): String = localProps.getProperty(key, default)

android {
    namespace = "com.hancekim.mynote"

    defaultConfig {
        applicationId = "com.hancekim.mynote"
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "GITHUB_OWNER", "\"${prop("github.owner", "KimHance")}\"")
        buildConfigField("String", "GITHUB_REPO", "\"${prop("github.repo", "MyNote")}\"")
        buildConfigField("String", "GITHUB_BRANCH", "\"${prop("github.branch", "main")}\"")
        buildConfigField("String", "GITHUB_TOKEN", "\"${prop("github.token", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    // composition root: domain(타입) + data(Hilt 바인딩 DataModule) 를 모두 포함해 그래프를 완성한다.
    implementation(projects.core.domain)
    implementation(projects.core.data)
    // composition root: 구현체 모듈을 포함해 Hilt 그래프를 완성한다.
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.feature.notes)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
}
