plugins {
    alias(libs.plugins.billboard.android.library.compose)
}

android {
    namespace = "com.hancekim.mynote.core.designsystem"
}

dependencies {
    // 토큰을 소비자(feature) 에 그대로 노출하기 위해 api 로 전파
    api(projects.core.designFoundation)
    // design-system 은 데이터 레이어를 의존하지 않는다. 블록 렌더러는 자체 UI 모델(NoteContentBlock)만 받는다.

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
