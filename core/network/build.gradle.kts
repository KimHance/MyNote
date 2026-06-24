plugins {
    alias(libs.plugins.billboard.android.library)
    alias(libs.plugins.billboard.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.network"
}

dependencies {
    // NotesDataSource 인터페이스 + DTO 를 구현/사용
    implementation(projects.core.data)
    // AppliedTokenSource 인터페이스(domain) — GithubTokenHolder 가 구현
    implementation(projects.core.domain)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
