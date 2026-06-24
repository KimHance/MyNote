plugins {
    alias(libs.plugins.billboard.android.library)
    alias(libs.plugins.billboard.android.room)
    alias(libs.plugins.billboard.android.hilt)
}

android {
    namespace = "com.hancekim.mynote.core.database"
}

dependencies {
    // CachedNote + LocalNotesDataSource 인터페이스를 구현/사용
    implementation(projects.core.data)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    // DataStore Preferences — 토큰 저장(DataStoreTokenStore)
    implementation(libs.androidx.datastore)
}
