import com.android.build.api.dsl.LibraryExtension
import com.hance.convention.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // AGP 9 는 Kotlin 이 빌트인이라 org.jetbrains.kotlin.android 적용이 불필요/금지.
                apply("com.android.library")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            // kotlin("test") 는 AGP 9 빌트인 Kotlin 에서 버전이 해석되지 않으므로 convention 에선 넣지 않는다.
            // 테스트 의존성은 각 모듈이 카탈로그(libs.junit 등)로 직접 선언한다.
        }
    }
}