plugins {
    alias(libs.plugins.billboard.android.application)
    alias(libs.plugins.billboard.android.application.compose)
    alias(libs.plugins.billboard.android.hilt)
}

android {
    namespace = "com.hancekim.mynote"

    defaultConfig {
        applicationId = "com.hancekim.mynote"
        versionCode = 1
        versionName = "1.0"
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
