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
import io.kotest.matchers.string.shouldNotContain

class GitHubDestinationTest : StringSpec() {
    private val repo = "https://github.com/owner/repo"

    init {
        "the repository destination is the bare repository URL" {
            GitHubDestination.REPOSITORY.urlFor(repo) shouldBe "https://github.com/owner/repo"
        }

        "pull requests append the pulls segment" {
            GitHubDestination.PULL_REQUESTS.urlFor(repo) shouldBe "https://github.com/owner/repo/pulls"
        }

        "issues append the issues segment" {
            GitHubDestination.ISSUES.urlFor(repo) shouldBe "https://github.com/owner/repo/issues"
        }

        "actions append the actions segment" {
            GitHubDestination.ACTIONS.urlFor(repo) shouldBe "https://github.com/owner/repo/actions"
        }

        "releases append the releases segment" {
            GitHubDestination.RELEASES.urlFor(repo) shouldBe "https://github.com/owner/repo/releases"
        }

        // The locator never produces a trailing slash today, but a destination URL with a doubled
        // slash would 404, and that is a silent failure in a browser rather than a build error.
        "a trailing slash on the repository URL does not double up" {
            GitHubDestination.ISSUES.urlFor("$repo/") shouldBe "https://github.com/owner/repo/issues"
            GitHubDestination.REPOSITORY.urlFor("$repo/") shouldBe "https://github.com/owner/repo"
        }

        "every destination builds on the URL it is given, with no doubled separator" {
            GitHubDestination.entries.forEach { destination ->
                destination.urlFor(repo) shouldBe "$repo${destination.pathSuffix}"
                // Checked past the scheme, so the "//" in "https://" is not what is being matched.
                destination.urlFor(repo).removePrefix("https://") shouldNotContain "//"
            }
        }

        "every destination has a distinct path" {
            val paths = GitHubDestination.entries.map { it.pathSuffix }
            paths.distinct().size shouldBe paths.size
        }
    }
}
