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
import io.kotest.matchers.shouldNotBe

/**
 * The service is instantiated directly rather than fetched through `service()`, which keeps these as
 * plain unit tests: nothing here needs a running Application.
 */
class GitHubHostSettingsTest : StringSpec() {
    init {
        "starts out recognizing only github.com" {
            GitHubHostSettings().allowedHosts shouldBe GitHubUrlParser.DEFAULT_HOSTS
        }

        "stores a configured host and adds it to the allowlist" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("github.mycompany.com"))

            settings.state.hosts.toList() shouldBe listOf("github.mycompany.com")
            settings.allowedHosts shouldBe GitHubUrlParser.DEFAULT_HOSTS + "github.mycompany.com"
        }

        "normalizes whatever the user typed before storing it" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("  HTTPS://GitHub.MyCompany.COM/owner/repo.git  "))

            settings.state.hosts.toList() shouldBe listOf("github.mycompany.com")
        }

        "drops entries that could never match a remote" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("github.mycompany.com", "", "   ", "has space.com", "*.wild.com"))

            settings.state.hosts.toList() shouldBe listOf("github.mycompany.com")
        }

        "drops duplicates, including ones that only differ before normalization" {
            val settings = GitHubHostSettings()
            settings.setHosts(
                listOf("github.mycompany.com", "GITHUB.MYCOMPANY.COM", "https://github.mycompany.com/x"),
            )

            settings.state.hosts.toList() shouldBe listOf("github.mycompany.com")
        }

        // github.com is unioned in unconditionally, so storing it again would be a redundant entry
        // that the user cannot remove any meaning from.
        "does not store the default hosts as if they were extras" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("github.com", "www.github.com", "github.mycompany.com"))

            settings.state.hosts.toList() shouldBe listOf("github.mycompany.com")
        }

        // The whole point of the allowlist: a bad entry can only fail to add a host.
        "a nonsense configuration cannot take github.com away" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("!!!", "  ", "///"))

            settings.state.hosts.toList() shouldBe emptyList()
            settings.allowedHosts shouldBe GitHubUrlParser.DEFAULT_HOSTS
        }

        "replaces rather than accumulates across calls" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("first.example.com"))
            settings.setHosts(listOf("second.example.com"))

            settings.state.hosts.toList() shouldBe listOf("second.example.com")
        }

        "clears back to the defaults when given nothing" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("github.mycompany.com"))
            settings.setHosts(emptyList())

            settings.state.hosts.toList() shouldBe emptyList()
            settings.allowedHosts shouldBe GitHubUrlParser.DEFAULT_HOSTS
        }

        // The IDE persists on modification count, so a real change has to bump it and a no-op
        // change must not — otherwise settings either fail to save or rewrite on every apply.
        "marks the state modified only when the hosts actually change" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("github.mycompany.com"))

            val afterChange = settings.state.modificationCount
            afterChange shouldNotBe 0L

            settings.setHosts(listOf("github.mycompany.com"))
            settings.state.modificationCount shouldBe afterChange
        }

        "produces hosts the parser then accepts" {
            val settings = GitHubHostSettings()
            settings.setHosts(listOf("https://GitHub.MyCompany.com/"))

            GitHubUrlParser.toRepoUrl(
                "git@github.mycompany.com:owner/repo.git",
                settings.allowedHosts,
            ) shouldBe "https://github.mycompany.com/owner/repo"
        }
    }
}
