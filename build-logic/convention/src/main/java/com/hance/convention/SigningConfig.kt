package com.hance.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

internal fun Project.configureReleaseSigning(extension: ApplicationExtension) {
    val localProperties = Properties().apply {
        val propsFile = rootProject.file("local.properties")
        if (propsFile.exists()) {
            FileInputStream(propsFile).use { load(it) }
        }
    }

    val keystorePath = localProperties.getProperty("keystore.path") ?: return
    val keystorePassword = localProperties.getProperty("keystore.password") ?: return
    val aliasName = localProperties.getProperty("key.alias") ?: return
    val aliasPassword = localProperties.getProperty("key.password") ?: return

    val keystoreFile = file(keystorePath)
    if (!keystoreFile.exists()) return

    extension.apply {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = aliasName
                keyPassword = aliasPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
