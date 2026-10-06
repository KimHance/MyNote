import com.android.build.api.dsl.LibraryExtension
import com.hancenote.convention.configureKotlinAndroid
import com.hancenote.convention.configureNotesTemplateInput
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("hancenote.ktlint")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            configureNotesTemplateInput()
        }
    }
}
