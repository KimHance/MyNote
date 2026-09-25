plugins {
    alias(libs.plugins.hancenote.android.library.compose)
}

android {
    resourcePrefix = "ds_"
    namespace = "com.hancekim.mynote.core.designsystem"
}

dependencies {
    api(projects.core.designFoundation)
    implementation(projects.core.common)
    api(libs.backdrop)

    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)
}
