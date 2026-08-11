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

class GitHubBranchDestinationTest : StringSpec() {
    private val repo = "https://github.com/owner/repo"

    init {
        "the branch destination is the tree view of that branch" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "main") shouldBe
                "https://github.com/owner/repo/tree/main"
        }

        "the pull request destination is GitHub's create-PR page for that branch" {
            GitHubBranchDestination.NEW_PULL_REQUEST.urlFor(repo, "feature-x") shouldBe
                "https://github.com/owner/repo/pull/new/feature-x"
        }

        // Slashes are ordinary in branch names and are path separators on GitHub, so they must
        // survive rather than being escaped into %2F.
        "a slash in a branch name stays a path separator" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "feature/nested/name") shouldBe
                "https://github.com/owner/repo/tree/feature/nested/name"
            GitHubBranchDestination.NEW_PULL_REQUEST.urlFor(repo, "feature/nested/name") shouldBe
                "https://github.com/owner/repo/pull/new/feature/nested/name"
        }

        "unreserved characters are left alone" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "release-1.2.3_final~ok") shouldBe
                "https://github.com/owner/repo/tree/release-1.2.3_final~ok"
        }

        // Git permits these in a ref name, and each would otherwise change how the URL parses.
        "characters that would break the URL are percent-encoded" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "fix#42") shouldBe
                "https://github.com/owner/repo/tree/fix%2342"
            GitHubBranchDestination.BRANCH.urlFor(repo, "a%b") shouldBe
                "https://github.com/owner/repo/tree/a%25b"
            GitHubBranchDestination.BRANCH.urlFor(repo, "what?now") shouldBe
                "https://github.com/owner/repo/tree/what%3Fnow"
        }

        "non-ascii branch names are encoded as UTF-8" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "fünf") shouldBe
                "https://github.com/owner/repo/tree/f%C3%BCnf"
        }

        "a blank branch name yields no URL" {
            GitHubBranchDestination.BRANCH.urlFor(repo, "") shouldBe null
            GitHubBranchDestination.BRANCH.urlFor(repo, "   ") shouldBe null
        }

        "a trailing slash on the repository URL does not double up" {
            GitHubBranchDestination.BRANCH.urlFor("$repo/", "main") shouldBe
                "https://github.com/owner/repo/tree/main"
        }

        "branch destinations build on an enterprise repository URL too" {
            GitHubBranchDestination.NEW_PULL_REQUEST.urlFor("https://github.mycompany.com/owner/repo", "main") shouldBe
                "https://github.mycompany.com/owner/repo/pull/new/main"
        }

        "every destination has a distinct path" {
            val paths = GitHubBranchDestination.entries.map { it.urlFor(repo, "b") }
            paths.distinct().size shouldBe paths.size
        }
    }
}
