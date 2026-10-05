package com.hancekim.mynote.core.domain.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun named(
    branch: String,
    path: String,
) = ConnectCandidate(ConnectBranch.Named(branch), path)

private val defaultCandidate = ConnectCandidate(ConnectBranch.Default, "notes/index.json")

private fun accepted(
    owner: String,
    repo: String,
    vararg candidates: ConnectCandidate,
) = ConnectLinkResult.Accepted(ConnectLinkRepo(owner, repo), candidates.toList())

private fun rejected(reason: ConnectLinkRejection) = ConnectLinkResult.Rejected(reason)

class ConnectLinkParserTest {
    @Test
    fun `RC-2 링크 모양별로 저장소와 후보 목록을 읽는다`() {
        val table =
            listOf(
                "owner/repo" to accepted("owner", "repo", defaultCandidate),
                "owner/repo.git" to accepted("owner", "repo", defaultCandidate),
                "owner/repo/" to accepted("owner", "repo", defaultCandidate),
                "github.com/owner/repo" to accepted("owner", "repo", defaultCandidate),
                "www.github.com/owner/repo" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo" to accepted("owner", "repo", defaultCandidate),
                "https://GitHub.com/owner/repo.git" to accepted("owner", "repo", defaultCandidate),
                "  https://github.com/owner/repo\n" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo?tab=readme-ov-file#readme" to
                    accepted(
                        "owner",
                        "repo",
                        defaultCandidate,
                    ),
                "git@github.com:owner/repo.git" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/issues/3" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/pull/7/files" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/releases" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/wiki" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/tree" to accepted("owner", "repo", defaultCandidate),
                "https://github.com/owner/repo/tree/main" to
                    accepted("owner", "repo", named("main", "notes/index.json")),
                "https://github.com/owner/repo/tree/main/docs" to
                    accepted("owner", "repo", named("main", "docs/index.json")),
                "https://github.com/owner/repo/tree/feature/x/docs" to
                    accepted(
                        "owner",
                        "repo",
                        named("feature/x", "docs/index.json"),
                        named("feature", "x/docs/index.json"),
                    ),
                "https://github.com/owner/repo/tree/a/b/c/d/e" to
                    accepted(
                        "owner",
                        "repo",
                        named("a/b/c/d", "e/index.json"),
                        named("a/b/c", "d/e/index.json"),
                        named("a/b", "c/d/e/index.json"),
                    ),
                "https://github.com/owner/repo/blob/main/index.json" to
                    accepted("owner", "repo", named("main", "index.json")),
                "https://github.com/owner/repo/blob/main/notes/index.json#L3" to
                    accepted("owner", "repo", named("main/notes", "index.json"), named("main", "notes/index.json")),
                "https://github.com/owner/repo/blob/feature/x/docs/index.json" to
                    accepted(
                        "owner",
                        "repo",
                        named("feature/x/docs", "index.json"),
                        named("feature/x", "docs/index.json"),
                        named("feature", "x/docs/index.json"),
                    ),
                "https://raw.githubusercontent.com/owner/repo/main/notes/index.json" to
                    accepted("owner", "repo", named("main/notes", "index.json"), named("main", "notes/index.json")),
                "raw.githubusercontent.com/owner/repo/main/index.json" to
                    accepted("owner", "repo", named("main", "index.json")),
                "my-org/my.repo_1" to accepted("my-org", "my.repo_1", defaultCandidate),
            )

        assertTrue(table.size >= 18)
        table.forEach { (input, expected) ->
            assertEquals(input, expected, parseConnectLink(input))
        }
    }

    @Test
    fun `RC-3 받지 않는 링크는 요청 없이 이유 7종이 다르다`() {
        val table =
            listOf(
                "" to ConnectLinkRejection.Empty,
                "   \n " to ConnectLinkRejection.Empty,
                "?tab=readme" to ConnectLinkRejection.Empty,
                "http://github.com/owner/repo" to ConnectLinkRejection.NotHttps,
                "example.com/foo" to ConnectLinkRejection.NotGithub,
                "https://example.com/foo/bar" to ConnectLinkRejection.NotGithub,
                "https://gitlab.com/owner/repo" to ConnectLinkRejection.NotGithub,
                "foo_bar/repo" to ConnectLinkRejection.NotGithub,
                "git@gitlab.com:owner/repo.git" to ConnectLinkRejection.NotGithub,
                "https://gist.github.com/owner/0123abcd" to ConnectLinkRejection.Gist,
                "https://owner.github.io/repo" to ConnectLinkRejection.GithubPages,
                "owner.github.io" to ConnectLinkRejection.GithubPages,
                "https://github.com/owner/repo/blob/main/README.md" to ConnectLinkRejection.SingleFile,
                "https://github.com/owner/repo/blob/main" to ConnectLinkRejection.SingleFile,
                "https://raw.githubusercontent.com/owner/repo/main/notes/a.json" to ConnectLinkRejection.SingleFile,
                "https://github.com/owner" to ConnectLinkRejection.MissingRepo,
                "https://github.com" to ConnectLinkRejection.MissingRepo,
                "owner" to ConnectLinkRejection.MissingRepo,
                "github.com/owner/" to ConnectLinkRejection.MissingRepo,
                "owner/.git" to ConnectLinkRejection.MissingRepo,
            )

        table.forEach { (input, reason) ->
            assertEquals(input, rejected(reason), parseConnectLink(input))
        }
        assertEquals(ConnectLinkRejection.entries.toSet(), table.map { it.second }.toSet())
        assertEquals(7, ConnectLinkRejection.entries.size)
    }

    @Test
    fun `RC-3 https 외 스킴은 전부 같은 이유다`() {
        listOf(
            "http://github.com/owner/repo",
            "ftp://github.com/owner/repo",
            "ssh://git@github.com/owner/repo.git",
            "git://github.com/owner/repo.git",
            "HTTP://github.com/owner/repo",
            "file:///owner/repo",
            "javascript:alert(1)",
            "ftp://example.com/foo",
        ).forEach { input ->
            assertEquals(input, rejected(ConnectLinkRejection.NotHttps), parseConnectLink(input))
        }
    }

    @Test
    fun `RC-2 후보는 최대 3개이고 긴 접두가 먼저다`() {
        val result = parseConnectLink("github.com/o/r/tree/a/b/c/d/e/f/g") as ConnectLinkResult.Accepted

        assertEquals(3, result.candidates.size)
        assertEquals(
            listOf("a/b/c/d/e/f", "a/b/c/d/e", "a/b/c/d"),
            result.candidates.map { (it.branch as ConnectBranch.Named).name },
        )
    }

    @Test
    fun `RC-2 owner 는 영숫자와 대시만 허용하고 점이 있으면 호스트로 읽는다`() {
        assertEquals(rejected(ConnectLinkRejection.NotGithub), parseConnectLink("foo.bar/repo"))
        assertEquals(rejected(ConnectLinkRejection.NotGithub), parseConnectLink("foo_bar/repo"))
        assertEquals(rejected(ConnectLinkRejection.NotGithub), parseConnectLink("https://github.com/foo.bar/repo"))
        assertEquals(accepted("foo-Bar9", "repo", defaultCandidate), parseConnectLink("foo-Bar9/repo"))
    }
}
