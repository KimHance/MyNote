import com.android.build.api.dsl.ApplicationExtension
import com.hancenote.convention.configureKotlinAndroid
import com.hancenote.convention.configureReleaseSigning
import com.hancenote.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("hancenote.ktlint")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                configureReleaseSigning(this)
            }
        }
    }
}
