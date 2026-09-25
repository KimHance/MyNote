plugins {
    alias(libs.plugins.hancenote.android.library)
    alias(libs.plugins.hancenote.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.ai"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)

    implementation(libs.litertlm.android)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
