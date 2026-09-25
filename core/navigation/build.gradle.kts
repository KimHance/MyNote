plugins {
    alias(libs.plugins.hancenote.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.navigation"
}

dependencies {
    implementation(projects.core.common)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
