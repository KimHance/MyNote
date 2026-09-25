plugins {
    alias(libs.plugins.hancenote.jvm.library)
}

dependencies {
    // domain 은 가장 안쪽 레이어 — 다른 모듈을 의존하지 않는다(DIP). 순수 Kotlin 만.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
