package com.hancekim.mynote.core.ai.download.transfer

import com.hancekim.mynote.core.ai.download.decision.DownloadPlan
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

internal class ModelFiles(dir: File, fileName: String) {
    val target: File = File(dir, fileName)
    val part: File = File(dir, "$fileName$PART_SUFFIX")
    val plan: File = File(dir, "$fileName$PLAN_SUFFIX")

    fun savedPlan(): DownloadPlan? =
        runCatching { json.decodeFromString<DownloadPlan>(plan.readText()) }.getOrNull()

    fun savePlan(value: DownloadPlan) {
        plan.parentFile?.mkdirs()
        plan.writeText(json.encodeToString(value))
    }

    fun partBytes(): Long = if (part.isFile) part.length() else 0L

    fun discardPart() {
        part.delete()
    }

    fun discardPlan() {
        plan.delete()
    }

    fun promote(): File {
        Files.move(
            part.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
        plan.delete()
        return target
    }

    fun matches(file: File, expected: DownloadPlan): Boolean =
        file.isFile && file.length() == expected.totalBytes && sha256Of(file) == expected.sha256

    internal companion object {
        const val PART_SUFFIX = ".part"
        const val PLAN_SUFFIX = ".plan"

        private const val HASH_BUFFER_BYTES = 64 * 1024

        private val json = Json { ignoreUnknownKeys = true }

        fun sha256Of(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(HASH_BUFFER_BYTES)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        }
    }
}
