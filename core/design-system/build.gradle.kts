plugins {
    alias(libs.plugins.billboard.android.library.compose)
}

android {
    namespace = "com.hancekim.mynote.core.designsystem"
}

dependencies {
    api(projects.core.designFoundation)
    api(libs.backdrop)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)

    testImplementation(libs.junit)
}
