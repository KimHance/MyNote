plugins {
    alias(libs.plugins.billboard.android.feature)
    alias(libs.plugins.billboard.android.hilt)
}

android {
    resourcePrefix = "notes_"
    namespace = "com.hancekim.mynote.feature.notes"
}

dependencies {
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation(libs.junit)
}
