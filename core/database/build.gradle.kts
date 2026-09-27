plugins {
    alias(libs.plugins.hancenote.android.library)
    alias(libs.plugins.hancenote.android.room)
    alias(libs.plugins.hancenote.android.hilt)
}

android {
    namespace = "com.hancekim.mynote.core.database"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
    implementation(libs.androidx.datastore)

    testImplementation(libs.junit)
}
