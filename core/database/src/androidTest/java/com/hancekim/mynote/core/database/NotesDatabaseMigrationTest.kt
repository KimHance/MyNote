package com.hancekim.mynote.core.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hancekim.mynote.core.database.migration.MIGRATION_4_5
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotesDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = instrumentation.targetContext.getDatabasePath(TEST_DB),
            driver = AndroidSQLiteDriver(),
            databaseClass = NotesDatabase::class,
        )

    @Before
    fun deleteOldDatabase() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun `v1 노트는 그대로 남고 학습 기록 테이블 넷이 생긴다`() =
        runTest {
            helper.createDatabase(1).use { connection ->
                connection.execSQL(
                    "INSERT INTO notes(id, group_name, title, date, contentHash, blocksJson) VALUES " +
                        "('graph', 'datastructure', '그래프', '2026.06.29', 'h1', '[]'), " +
                        "('nav3', 'android', 'Navigation 3', '2026.06.18', 'h2', '[{\"type\":\"divider\"}]')",
                )
            }

            helper.runMigrationsAndValidate(2).use { connection ->
                assertEquals(
                    listOf("graph|datastructure|그래프|h1", "nav3|android|Navigation 3|h2"),
                    connection.rows(
                        "SELECT id || '|' || group_name || '|' || title || '|' || contentHash FROM notes ORDER BY id",
                    ),
                )
                assertEquals(
                    listOf("bookmark_events", "quiz_answers", "quiz_attempts", "read_sessions"),
                    connection.rows(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                            "('read_sessions', 'quiz_attempts', 'quiz_answers', 'bookmark_events') ORDER BY name",
                    ),
                )
                assertEquals(
                    listOf(
                        "index_bookmark_events_at",
                        "index_quiz_attempts_finished_at",
                        "index_quiz_attempts_note_id",
                        "index_read_sessions_note_id",
                        "index_read_sessions_started_at",
                    ),
                    connection.rows(
                        "SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%' ORDER BY name",
                    ),
                )
            }
        }

    @Test
    fun `v2 로 올린 DB 에 학습 기록을 쓰고 답은 시도와 함께 지워진다`() =
        runTest {
            helper.createDatabase(1).close()

            helper.runMigrationsAndValidate(2).use { connection ->
                connection.execSQL("PRAGMA foreign_keys = ON")
                connection.execSQL(
                    "INSERT INTO read_sessions(note_id, category, started_at, ended_at, active_ms, max_percent) " +
                        "VALUES ('graph', 'datastructure', 1000, 41000, 40000, 78)",
                )
                connection.execSQL(
                    "INSERT INTO quiz_attempts(note_id, category, started_at, finished_at, asked, correct) " +
                        "VALUES ('graph', 'datastructure', 1000, 9000, 3, 2)",
                )
                connection.execSQL(
                    "INSERT INTO quiz_answers(attempt_id, position, question, " +
                        "choice_0, choice_1, choice_2, choice_3, " +
                        "answer_index, picked_index) VALUES (1, 0, 'q', 'a', 'b', 'c', NULL, 0, 1)",
                )
                connection.execSQL(
                    "INSERT INTO bookmark_events(note_id, category, kind, at) " +
                        "VALUES ('graph', 'datastructure', 'added', 5)",
                )

                assertEquals(
                    listOf("graph|78"),
                    connection.rows("SELECT note_id || '|' || max_percent FROM read_sessions"),
                )
                assertEquals(listOf("added"), connection.rows("SELECT kind FROM bookmark_events"))

                connection.execSQL("DELETE FROM quiz_attempts WHERE id = 1")

                assertEquals(listOf("0"), connection.rows("SELECT COUNT(*) FROM quiz_answers"))
            }
        }

    @Test
    fun `v2 기록은 v3 v4 를 거쳐도 남고 요약과 패턴 테이블이 생긴다`() =
        runTest {
            helper.createDatabase(2).use { connection ->
                connection.execSQL(
                    "INSERT INTO read_sessions(note_id, category, started_at, ended_at, active_ms, max_percent) " +
                        "VALUES ('graph', 'datastructure', 1000, 41000, 40000, 78)",
                )
            }

            helper.runMigrationsAndValidate(3).use { connection ->
                connection.execSQL(
                    "INSERT INTO weekly_summaries(week_start, generated_at, headline, detail, focus, model_commit, " +
                        "schema_version) VALUES (20725, 5, 'h', 'd', NULL, 'c', 1)",
                )
                connection.execSQL(
                    "INSERT OR REPLACE INTO weekly_summaries(week_start, generated_at, headline, detail, focus, " +
                        "model_commit, schema_version) VALUES (20725, 9, 'h2', 'd2', 'cs', 'c', 1)",
                )
                assertEquals(listOf("h2|cs"), connection.rows("SELECT headline || '|' || focus FROM weekly_summaries"))
            }

            helper.runMigrationsAndValidate(4).use { connection ->
                connection.execSQL(
                    "INSERT INTO quiz_patterns(week_start, generated_at, tag_0, tag_1, tag_2, sentence, " +
                        "model_commit, schema_version) VALUES (20725, 7, '스택', NULL, NULL, 's', 'c', 1)",
                )
                assertEquals(
                    listOf("graph|78"),
                    connection.rows("SELECT note_id || '|' || max_percent FROM read_sessions"),
                )
                assertEquals(listOf("h2"), connection.rows("SELECT headline FROM weekly_summaries"))
                assertEquals(listOf("스택"), connection.rows("SELECT tag_0 FROM quiz_patterns"))
            }
        }

    @Test
    fun `v1 에서 v5 까지 한 번에 올라간다`() =
        runTest {
            helper.createDatabase(1).use { connection ->
                connection.execSQL(
                    "INSERT INTO notes(id, group_name, title, date, contentHash, blocksJson) VALUES " +
                        "('graph', 'datastructure', '그래프', '2026.06.29', 'h1', '[]')",
                )
            }

            helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).use { connection ->
                assertEquals(
                    listOf("s0/graph|graph|h1"),
                    connection.rows("SELECT note_key || '|' || doc_id || '|' || revision FROM source_notes"),
                )
                assertEquals(
                    listOf("quiz_patterns", "source_notes", "sources", "weekly_summaries"),
                    connection.rows(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                            "('weekly_summaries', 'quiz_patterns', 'sources', 'source_notes', 'notes') ORDER BY name",
                    ),
                )
            }
        }

    @Test
    fun `v4 노트는 source_notes 로 옮겨지고 s0 소스가 생긴다`() =
        runTest {
            helper.createDatabase(4).use { connection ->
                connection.execSQL(
                    "INSERT INTO notes(id, group_name, title, date, contentHash, blocksJson) VALUES " +
                        "('graph', 'datastructure', '그래프', '2026.06.29', 'h1', '[]'), " +
                        "('nav3', 'android', 'Navigation 3', '2026.06.18', 'h2', '[{\"type\":\"divider\"}]'), " +
                        "('http', 'cs', 'HTTP 캐싱 기초', '2026.04.22', 'h3', '[{\"type\":\"unknown\"}]')",
                )
            }

            helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).use { connection ->
                assertEquals(
                    listOf(
                        "s0/graph|s0|graph|datastructure|그래프|2026.06.29|h1|[]",
                        "s0/http|s0|http|cs|HTTP 캐싱 기초|2026.04.22|h3|[{\"type\":\"unknown\"}]",
                        "s0/nav3|s0|nav3|android|Navigation 3|2026.06.18|h2|[{\"type\":\"divider\"}]",
                    ),
                    connection.rows(
                        "SELECT note_key || '|' || source_id || '|' || doc_id || '|' || group_name || '|' || " +
                            "title || '|' || " +
                            "date || '|' || revision || '|' || blocks_json FROM source_notes ORDER BY note_key",
                    ),
                )
                assertEquals(
                    listOf("3"),
                    connection.rows(
                        "SELECT COUNT(*) FROM source_notes WHERE raw IS NULL AND parser_version = 0 AND path IS NULL " +
                            "AND fetched_at IS NULL AND excerpt IS NULL AND kind IS NULL AND summary IS NULL " +
                            "AND aliases_json IS NULL AND source_url IS NULL " +
                            "AND tags_json IS NULL AND warnings_json IS NULL " +
                            "AND first_seen_at > 0",
                    ),
                )
                assertEquals(
                    listOf("s0|github|1"),
                    connection.rows("SELECT id || '|' || kind || '|' || (added_at > 0) FROM sources"),
                )
                assertEquals(
                    listOf("0"),
                    connection.rows("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'notes'"),
                )
            }
        }

    @Test
    fun `학습 기록 세 테이블의 note_id 에 s0 접두가 붙고 답과 요약 패턴은 그대로다`() =
        runTest {
            helper.createDatabase(4).use { connection ->
                connection.execSQL(
                    "INSERT INTO read_sessions(note_id, category, started_at, ended_at, active_ms, max_percent) " +
                        "VALUES ('graph', 'datastructure', 1000, 41000, 40000, 78), ('nav3', 'android', 2000, 9000, 7000, 10)",
                )
                connection.execSQL(
                    "INSERT INTO quiz_attempts(note_id, category, started_at, finished_at, asked, correct) " +
                        "VALUES ('graph', 'datastructure', 1000, 9000, 3, 2)",
                )
                connection.execSQL(
                    "INSERT INTO quiz_answers(attempt_id, position, question, " +
                        "choice_0, choice_1, choice_2, choice_3, " +
                        "answer_index, picked_index) VALUES (1, 0, 'q0', 'a', 'b', 'c', NULL, 0, 1), " +
                        "(1, 1, 'q1', 'a', 'b', 'c', 'd', 2, 2)",
                )
                connection.execSQL(
                    "INSERT INTO bookmark_events(note_id, category, kind, at) VALUES ('graph', 'datastructure', 'added', 5)",
                )
                connection.execSQL(
                    "INSERT INTO weekly_summaries(week_start, generated_at, headline, detail, focus, model_commit, " +
                        "schema_version) VALUES (20725, 5, 'h', 'd', NULL, 'c', 1)",
                )
                connection.execSQL(
                    "INSERT INTO quiz_patterns(week_start, generated_at, tag_0, tag_1, tag_2, sentence, " +
                        "model_commit, schema_version) VALUES (20725, 7, '스택', NULL, NULL, 's', 'c', 1)",
                )
            }

            helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).use { connection ->
                assertEquals(
                    listOf("s0/graph|78", "s0/nav3|10"),
                    connection.rows("SELECT note_id || '|' || max_percent FROM read_sessions ORDER BY note_id"),
                )
                assertEquals(
                    listOf("s0/graph|llm"),
                    connection.rows("SELECT note_id || '|' || format FROM quiz_attempts"),
                )
                assertEquals(
                    listOf("s0/graph|added"),
                    connection.rows("SELECT note_id || '|' || kind FROM bookmark_events"),
                )
                assertEquals(
                    listOf("1|q0", "1|q1"),
                    connection.rows("SELECT attempt_id || '|' || question FROM quiz_answers ORDER BY position"),
                )
                assertEquals(listOf("h"), connection.rows("SELECT headline FROM weekly_summaries"))
                assertEquals(listOf("스택"), connection.rows("SELECT tag_0 FROM quiz_patterns"))

                connection.execSQL("PRAGMA foreign_keys = ON")
                connection.execSQL("DELETE FROM quiz_attempts WHERE id = 1")
                assertEquals(listOf("0"), connection.rows("SELECT COUNT(*) FROM quiz_answers"))
            }
        }

    @Test
    fun `v4 의 날짜가 epoch_day 로 바뀌고 형식이 다르면 NULL 이다`() =
        runTest {
            helper.createDatabase(4).use { connection ->
                connection.execSQL(
                    "INSERT INTO notes(id, group_name, title, date, contentHash, blocksJson) VALUES " +
                        "('dot', 'g', 't', '2026.06.29', 'h', '[]'), ('dash', 'g', 't', '2026-06-29', 'h', '[]'), " +
                        "('bad', 'g', 't', '어제', 'h', '[]')",
                )
            }

            helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).use { connection ->
                assertEquals(
                    listOf("bad|null", "dash|20633", "dot|20633"),
                    connection.rows(
                        "SELECT doc_id || '|' || ifnull(epoch_day, 'null') FROM source_notes ORDER BY doc_id",
                    ),
                )
            }
        }

    @Test
    fun `같은 날짜 노트의 v4 순서가 v5 에서도 같다`() =
        runTest {
            val before =
                helper.createDatabase(4).use { connection ->
                    connection.execSQL(
                        "INSERT INTO notes(id, group_name, title, date, contentHash, blocksJson) VALUES " +
                            "('c', 'g', 'c', '2026.06.29', 'h', '[]'), ('old', 'g', 'old', '2026.06.18', 'h', '[]'), " +
                            "('a', 'g', 'a', '2026.06.29', 'h', '[]'), ('b', 'g', 'b', '2026.06.29', 'h', '[]')",
                    )
                    connection.rows("SELECT id FROM notes ORDER BY date DESC")
                }

            helper.runMigrationsAndValidate(5, listOf(MIGRATION_4_5)).use { connection ->
                assertEquals(listOf("c", "a", "b", "old"), before)
                assertEquals(
                    before,
                    connection.rows(
                        "SELECT doc_id FROM source_notes " +
                            "ORDER BY epoch_day IS NULL, epoch_day DESC, first_seen_at DESC, title",
                    ),
                )
            }
        }

    private fun SQLiteConnection.rows(sql: String): List<String> =
        prepare(sql).use { statement ->
            buildList {
                while (statement.step()) add(statement.getText(0))
            }
        }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
