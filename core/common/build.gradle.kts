plugins {
    alias(libs.plugins.billboard.android.library)
}

android {
    namespace = "com.hancekim.mynote.core.common"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
