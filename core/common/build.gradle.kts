plugins {
    // Compose 아님 — 상태 생산 base 는 Flow 만 쓴다(Compose 런타임 없이 유닛테스트).
    alias(libs.plugins.billboard.android.library)
}

android {
    namespace = "com.hancekim.mynote.core.common"
}

dependencies {
    // common 은 leaf — 다른 :core:* 를 의존하지 않는다. Android UI/Compose 의존도 두지 않는다.
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
