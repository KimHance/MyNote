plugins {
    alias(libs.plugins.billboard.android.library)
    alias(libs.plugins.billboard.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.data"
}

dependencies {
    // data 는 domain 을 의존해 그 Repository 인터페이스를 구현한다(DIP). 의존 방향: data → domain.
    implementation(projects.core.domain)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
