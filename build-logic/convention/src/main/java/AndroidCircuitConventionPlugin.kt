import com.hance.convention.configureCircuit
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidCircuitConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureCircuit()
        }
    }
}