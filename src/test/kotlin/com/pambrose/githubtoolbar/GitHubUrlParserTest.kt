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
    }
}
