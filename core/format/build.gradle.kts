plugins {
    alias(libs.plugins.hancenote.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.domain)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
}
