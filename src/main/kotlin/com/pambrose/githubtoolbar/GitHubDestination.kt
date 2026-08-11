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

/**
 * A page within a GitHub repository that this plugin can open.
 *
 * Each destination is a fixed path below the repository URL, so a destination URL inherits whatever
 * [GitHubUrlParser] already validated: the host was matched exactly, and any embedded credentials
 * were discarded. Nothing here parses or re-derives a host, and nothing here is caller-supplied —
 * keep it that way, or the guarantees made one layer down stop holding.
 *
 * Deliberately free of IntelliJ Platform dependencies so it can be unit tested directly.
 */
enum class GitHubDestination(
    segment: String,
) {
    REPOSITORY(""),
    PULL_REQUESTS("pulls"),
    ISSUES("issues"),
    ACTIONS("actions"),
    RELEASES("releases"),
    ;

    /** Empty for the repository home page, `/pulls` and so on for the rest. */
    val pathSuffix: String = if (segment.isEmpty()) "" else "/$segment"

    /**
     * Returns the URL of this destination within [repoUrl], which must be a repository URL produced
     * by [GitHubUrlParser.toRepoUrl].
     */
    fun urlFor(repoUrl: String): String = repoUrl.trimEnd('/') + pathSuffix
}
