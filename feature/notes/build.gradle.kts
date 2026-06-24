plugins {
    alias(libs.plugins.billboard.android.feature)
    alias(libs.plugins.billboard.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hancekim.mynote.feature.notes"
}

dependencies {
    // feature convention 이 :core:design-system, :core:data, lifecycle, coroutines 를 이미 추가함

    // Navigation 3 + Material3 adaptive (목록-상세)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)

    // Hilt ViewModel 진입(hiltViewModel)
    implementation(libs.androidx.hilt.navigation.compose)

    // NavKey @Serializable
    implementation(libs.kotlinx.serialization.json)

    // 토큰 다이얼로그 아이콘(Lock) — compose BOM 관리, material-icons-core
    implementation(libs.androidx.compose.material.icons.core)
}
