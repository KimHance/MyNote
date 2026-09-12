plugins {
    alias(libs.plugins.billboard.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
