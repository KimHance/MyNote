plugins {
    alias(libs.plugins.hancenote.android.library)
    alias(libs.plugins.hancenote.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.network"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
