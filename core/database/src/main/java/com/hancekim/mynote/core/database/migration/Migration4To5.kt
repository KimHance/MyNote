package com.hancekim.mynote.core.database.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override suspend fun migrate(connection: SQLiteConnection) {
            MIGRATION_4_5_STATEMENTS.forEach { connection.execSQL(it) }
        }
    }

private const val NOW_MILLIS = "CAST(strftime('%s','now') AS INTEGER) * 1000"

internal val MIGRATION_4_5_STATEMENTS: List<String> =
    listOf(
        "CREATE TABLE IF NOT EXISTS `sources` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
            "`config_json` TEXT, `added_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "INSERT INTO `sources` (`id`, `kind`, `config_json`, `added_at`) VALUES ('s0', 'github', NULL, $NOW_MILLIS)",
        "CREATE TABLE IF NOT EXISTS `source_notes` (`note_key` TEXT NOT NULL, `source_id` TEXT NOT NULL, " +
            "`doc_id` TEXT NOT NULL, `path` TEXT, `group_name` TEXT NOT NULL, `title` TEXT NOT NULL, `date` TEXT, " +
            "`epoch_day` INTEGER, `first_seen_at` INTEGER NOT NULL, `revision` TEXT, `fetched_at` INTEGER, " +
            "`parser_version` INTEGER NOT NULL, `raw` TEXT, `blocks_json` TEXT NOT NULL, `excerpt` TEXT, " +
            "`kind` TEXT, `summary` TEXT, `aliases_json` TEXT, `source_url` TEXT, `tags_json` TEXT, " +
            "`warnings_json` TEXT, PRIMARY KEY(`note_key`))",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_source_notes_source_id_doc_id` " +
            "ON `source_notes` (`source_id`, `doc_id`)",
        "INSERT INTO `source_notes` (`note_key`, `source_id`, `doc_id`, `path`, `group_name`, `title`, `date`, " +
            "`epoch_day`, `first_seen_at`, `revision`, `fetched_at`, `parser_version`, `raw`, `blocks_json`, " +
            "`excerpt`) " +
            "SELECT 's0/' || `id`, 's0', `id`, NULL, `group_name`, `title`, `date`, " +
            "CAST(julianday(replace(`date`, '.', '-')) - 2440587.5 AS INTEGER), " +
            "$NOW_MILLIS - `rowid`, `contentHash`, " +
            "NULL, 0, NULL, `blocksJson`, NULL FROM `notes`",
        "DROP TABLE `notes`",
        "UPDATE `read_sessions` SET `note_id` = 's0/' || `note_id` WHERE instr(`note_id`, '/') = 0",
        "UPDATE `quiz_attempts` SET `note_id` = 's0/' || `note_id` WHERE instr(`note_id`, '/') = 0",
        "UPDATE `bookmark_events` SET `note_id` = 's0/' || `note_id` WHERE instr(`note_id`, '/') = 0",
        "ALTER TABLE `quiz_attempts` ADD COLUMN `format` TEXT NOT NULL DEFAULT 'llm'",
    )
