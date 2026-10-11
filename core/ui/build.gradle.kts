plugins {
    alias(libs.plugins.hancenote.android.library.compose)
}

android {
    resourcePrefix = "ui_"
    namespace = "com.hancekim.mynote.core.ui"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designSystem)
    implementation(projects.core.domain)
    implementation(projects.core.navigation)
}
