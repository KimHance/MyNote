plugins {
    alias(libs.plugins.billboard.android.library)
}

android {
    namespace = "com.hancekim.mynote.core.domain"
}

dependencies {
    // domain 은 가장 안쪽 레이어 — 다른 모듈을 의존하지 않는다(DIP). 순수 Kotlin 만.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
