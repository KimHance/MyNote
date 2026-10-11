plugins {
    alias(libs.plugins.hancenote.android.library.compose)
}

android {
    resourcePrefix = "ui_"
    namespace = "com.hancekim.mynote.core.ui"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designSystem)
    implementation(projects.core.domain)
    implementation(projects.core.navigation)
    // 신호 계약 · 마지막 동기화 홀더의 `@Inject` 생성자 — Hilt 플러그인 없이 애너테이션만(:core:domain 과 같다)
    implementation(libs.javax.inject)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation(libs.junit)
}
