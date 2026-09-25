package com.hancenote.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

private val SIGNING_KEYS =
    listOf("keystore.path", "keystore.password", "key.alias", "key.password")

/**
 * 키스토어가 **아예 없는 것**과 **하나 빠진 것**은 다른 상황이다.
 *
 * 앞은 키를 안 가진 개발자라 조용히 넘어가는 게 맞다. 뒤는 오타나 경로
 * 이동이고, 조용히 넘어가면 `BUILD SUCCESSFUL` 뒤에 서명 없는 APK 가
 * 남는다 — 설치하거나 업로드하는 순간에야 터지는데 그때는 원인에서
 * 멀리 와 있다. 그래서 뒤쪽만 말해 준다.
 */
internal fun Project.configureReleaseSigning(extension: ApplicationExtension) {
    val localProperties = Properties().apply {
        val propsFile = rootProject.file("local.properties")
        if (propsFile.exists()) {
            FileInputStream(propsFile).use { load(it) }
        }
    }

    val present = SIGNING_KEYS.filter { !localProperties.getProperty(it).isNullOrBlank() }
    if (present.isEmpty()) return
    if (present.size < SIGNING_KEYS.size) {
        logger.warn(
            "release 서명 건너뜀 — local.properties 에 {} 없음",
            (SIGNING_KEYS - present.toSet()).joinToString(),
        )
        return
    }

    val keystoreFile = file(localProperties.getProperty("keystore.path"))
    if (!keystoreFile.exists()) {
        logger.warn("release 서명 건너뜀 — 키스토어 파일 없음: {}", keystoreFile)
        return
    }

    extension.apply {
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = localProperties.getProperty("keystore.password")
                keyAlias = localProperties.getProperty("key.alias")
                keyPassword = localProperties.getProperty("key.password")
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}
