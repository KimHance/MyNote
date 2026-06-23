import com.android.build.api.dsl.TestExtension
import com.hance.convention.configureKotlin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.test")
                apply("org.jetbrains.kotlin.android")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<TestExtension> {
                compileSdk = Integer.parseInt(libs.findVersion("compileSdk").get().requiredVersion)

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                defaultConfig {
                    minSdk = Integer.parseInt(libs.findVersion("minSdk").get().requiredVersion)
                    targetSdk = Integer.parseInt(libs.findVersion("targetSdk").get().requiredVersion)
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            configureKotlin()
        }
    }
}
