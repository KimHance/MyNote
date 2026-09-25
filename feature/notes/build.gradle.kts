plugins {
    alias(libs.plugins.hancenote.android.feature)
    alias(libs.plugins.hancenote.android.hilt)
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
