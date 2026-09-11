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
                apply("com.android.application")
                apply("hance.ktlint")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                configureReleaseSigning(this)
            }
        }
    }
}
