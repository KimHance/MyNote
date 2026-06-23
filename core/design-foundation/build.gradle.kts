plugins {
    alias(libs.plugins.billboard.android.library.compose)
}

android {
    namespace = "com.hancekim.mynote.core.designfoundation"
}

dependencies {
    // 디자인 토큰(Color/Dp)·WindowSizeClass·마크다운 파서. Compose 런타임/UI 는 compose convention 이 제공.
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
}
