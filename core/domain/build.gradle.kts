plugins {
    alias(libs.plugins.billboard.android.library)
}

android {
    namespace = "com.hancekim.mynote.core.domain"
}

dependencies {
    // 모델 + Repository 인터페이스를 소비자(feature)에 전파
    api(projects.core.data)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
