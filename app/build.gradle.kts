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
    // :core:data 의 Hilt 모듈(DataModule)은 :core:domain → api(:core:data) 로 전파되어 그래프에 포함된다.
    implementation(projects.core.domain)
    implementation(projects.feature.notes)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
}
