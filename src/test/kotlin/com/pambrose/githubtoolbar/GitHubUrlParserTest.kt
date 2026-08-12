/*
 * Copyright 2026 Paul Ambrose
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pambrose.githubtoolbar

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GitHubUrlParserTest : StringSpec() {
    init {
        "parses an https remote with a .git suffix" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "parses an https remote without a .git suffix" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/repo") shouldBe
                "https://github.com/owner/repo"
        }

        "parses an scp-style ssh remote" {
            GitHubUrlParser.toRepoUrl("git@github.com:owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "parses an scp-style ssh remote without a .git suffix" {
            GitHubUrlParser.toRepoUrl("git@github.com:owner/repo") shouldBe
                "https://github.com/owner/repo"
        }

        "parses an ssh:// remote" {
            GitHubUrlParser.toRepoUrl("ssh://git@github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "parses a git:// remote" {
            GitHubUrlParser.toRepoUrl("git://github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "parses an http remote and upgrades it to https" {
            GitHubUrlParser.toRepoUrl("http://github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "strips embedded credentials so they never reach the browser" {
            GitHubUrlParser.toRepoUrl("https://user:ghp_secrettoken@github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "strips a bare username with no password" {
            GitHubUrlParser.toRepoUrl("https://user@github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "ignores a trailing slash" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/repo/") shouldBe
                "https://github.com/owner/repo"
        }

        "ignores surrounding whitespace" {
            GitHubUrlParser.toRepoUrl("  https://github.com/owner/repo.git  ") shouldBe
                "https://github.com/owner/repo"
        }

        "accepts a www.github.com host" {
            GitHubUrlParser.toRepoUrl("https://www.github.com/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "normalizes host casing but preserves owner and repo casing" {
            GitHubUrlParser.toRepoUrl("https://GitHub.COM/Owner/RepoName.git") shouldBe
                "https://github.com/Owner/RepoName"
        }

        "preserves dots and dashes in the repository name" {
            GitHubUrlParser.toRepoUrl("git@github.com:my-org/my.repo-name.git") shouldBe
                "https://github.com/my-org/my.repo-name"
        }

        "keeps a repository named exactly .git intact" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/.git.git") shouldBe
                "https://github.com/owner/.git"
        }

        "ignores an ssh port" {
            GitHubUrlParser.toRepoUrl("ssh://git@github.com:22/owner/repo.git") shouldBe
                "https://github.com/owner/repo"
        }

        "rejects a non-GitHub host" {
            GitHubUrlParser.toRepoUrl("https://gitlab.com/owner/repo.git") shouldBe null
        }

        "rejects a self-hosted GitHub Enterprise host" {
            GitHubUrlParser.toRepoUrl("https://github.mycompany.com/owner/repo.git") shouldBe null
        }

        "rejects a host that merely ends in github.com" {
            GitHubUrlParser.toRepoUrl("https://evilgithub.com/owner/repo.git") shouldBe null
        }

        "rejects a lookalike host that prefixes github.com" {
            GitHubUrlParser.toRepoUrl("https://github.com.evil.example/owner/repo.git") shouldBe null
        }

        "rejects a remote with no repository segment" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner") shouldBe null
        }

        "rejects a remote with an empty repository segment" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/") shouldBe null
        }

        "rejects a remote with an empty owner segment" {
            GitHubUrlParser.toRepoUrl("https://github.com//repo.git") shouldBe null
        }

        "rejects a blank string" {
            GitHubUrlParser.toRepoUrl("   ") shouldBe null
        }

        "rejects unparseable junk" {
            GitHubUrlParser.toRepoUrl("not a url at all") shouldBe null
        }

        "rejects a local filesystem remote" {
            GitHubUrlParser.toRepoUrl("/Users/someone/git/repo") shouldBe null
        }

        // ---- GitHub Enterprise: additional hosts supplied by the user ----

        "accepts a configured enterprise host and keeps that host in the result" {
            GitHubUrlParser.toRepoUrl(
                "https://github.mycompany.com/owner/repo.git",
                setOf("github.mycompany.com"),
            ) shouldBe "https://github.mycompany.com/owner/repo"
        }

        "accepts an scp-style remote on a configured enterprise host" {
            GitHubUrlParser.toRepoUrl(
                "git@github.mycompany.com:owner/repo.git",
                setOf("github.mycompany.com"),
            ) shouldBe "https://github.mycompany.com/owner/repo"
        }

        "strips credentials from an enterprise remote too" {
            GitHubUrlParser.toRepoUrl(
                "https://user:token@github.mycompany.com/owner/repo.git",
                setOf("github.mycompany.com"),
            ) shouldBe "https://github.mycompany.com/owner/repo"
        }

        // The whole point of the allowlist is that it adds hosts without weakening the match.
        "a configured host does not admit its own lookalikes" {
            val hosts = setOf("github.mycompany.com")
            GitHubUrlParser.toRepoUrl("https://evilgithub.mycompany.com/owner/repo.git", hosts) shouldBe null
            GitHubUrlParser.toRepoUrl("https://github.mycompany.com.evil.example/owner/repo.git", hosts) shouldBe null
            GitHubUrlParser.toRepoUrl("https://mycompany.com/owner/repo.git", hosts) shouldBe null
        }

        "configuring a host does not silently drop github.com itself" {
            GitHubUrlParser.toRepoUrl(
                "https://github.com/owner/repo.git",
                GitHubUrlParser.DEFAULT_HOSTS + "github.mycompany.com",
            ) shouldBe "https://github.com/owner/repo"
        }

        "an enterprise host is matched case-insensitively" {
            GitHubUrlParser.toRepoUrl(
                "https://GitHub.MyCompany.COM/Owner/Repo.git",
                setOf("github.mycompany.com"),
            ) shouldBe "https://github.mycompany.com/Owner/Repo"
        }

        // www.github.com is a real alias for github.com; www.<enterprise> is not known to be one,
        // so it is left alone rather than guessed at.
        "only github.com has its www alias folded away" {
            GitHubUrlParser.toRepoUrl(
                "https://www.github.mycompany.com/owner/repo.git",
                setOf("www.github.mycompany.com"),
            ) shouldBe "https://www.github.mycompany.com/owner/repo"
        }

        "an empty host set accepts nothing" {
            GitHubUrlParser.toRepoUrl("https://github.com/owner/repo.git", emptySet()) shouldBe null
        }

        // ---- Normalizing what the user typed into the settings field ----

        "normalizes a pasted URL down to its host" {
            GitHubUrlParser.normalizeHost("https://github.mycompany.com/owner/repo") shouldBe
                "github.mycompany.com"
        }

        "normalizes casing, surrounding space, credentials, a path, and a port" {
            GitHubUrlParser.normalizeHost("  GitHub.MyCompany.COM  ") shouldBe "github.mycompany.com"
            GitHubUrlParser.normalizeHost("git@github.mycompany.com") shouldBe "github.mycompany.com"
            // What `git remote -v` prints for an scp-style remote, so the likeliest paste of all.
            GitHubUrlParser.normalizeHost("git@github.mycompany.com:owner/repo.git") shouldBe "github.mycompany.com"
            // Pins the *order* of the reduction: the path is cut away first, so a later '@'
            // cannot be mistaken for the end of the credentials. Strip credentials first and this
            // returns "po", which is a well-formed hostname and so fails silently.
            GitHubUrlParser.normalizeHost("https://github.mycompany.com/owner/re@po") shouldBe "github.mycompany.com"
            GitHubUrlParser.normalizeHost("github.mycompany.com:8443") shouldBe "github.mycompany.com"
            GitHubUrlParser.normalizeHost("ssh://git@github.mycompany.com:22/owner/repo.git") shouldBe
                "github.mycompany.com"
        }

        "rejects host input that could never match anyway" {
            GitHubUrlParser.normalizeHost("") shouldBe null
            GitHubUrlParser.normalizeHost("   ") shouldBe null
            GitHubUrlParser.normalizeHost("/") shouldBe null
            GitHubUrlParser.normalizeHost("has space.com") shouldBe null
            GitHubUrlParser.normalizeHost("-leading.dash.com") shouldBe null
            GitHubUrlParser.normalizeHost("trailing.dot.") shouldBe null
            GitHubUrlParser.normalizeHost("*.wildcard.com") shouldBe null
        }

        "a normalized host round-trips into a working allowlist entry" {
            val host = GitHubUrlParser.normalizeHost("https://GitHub.MyCompany.com/")
            host shouldBe "github.mycompany.com"
            GitHubUrlParser.toRepoUrl("git@github.mycompany.com:owner/repo.git", setOfNotNull(host)) shouldBe
                "https://github.mycompany.com/owner/repo"
        }

        // Both the settings store and the Settings page's Apply button derive their list through
        // this one function. Two separate derivations would have to agree exactly or Apply would
        // either never settle or never enable, so the reduction is pinned here in one place.
        "reduces a block of typed lines to exactly what the settings store" {
            GitHubUrlParser.normalizeHosts(
                listOf(
                    "  HTTPS://GitHub.MyCompany.COM/owner/repo.git  ",
                    "github.com",
                    "www.github.com",
                    "!!!",
                    "",
                    "GITHUB.MYCOMPANY.COM",
                    "second.example.com",
                ),
            ) shouldBe listOf("github.mycompany.com", "second.example.com")
        }
    }
}
