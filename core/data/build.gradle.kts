plugins {
    alias(libs.plugins.billboard.android.library)
    alias(libs.plugins.billboard.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.common)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
