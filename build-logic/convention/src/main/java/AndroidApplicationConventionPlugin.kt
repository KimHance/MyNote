import com.android.build.api.dsl.ApplicationExtension
import com.hance.convention.configureKotlinAndroid
import com.hance.convention.configureReleaseSigning
import com.hance.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // AGP 9 는 Kotlin 이 빌트인이라 org.jetbrains.kotlin.android 적용이 불필요/금지.
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                // compileSdk / minSdk 는 configureKotlinAndroid 가 설정. 여기선 targetSdk 만.
                defaultConfig.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                configureReleaseSigning(this)
            }
        }
    }
}
