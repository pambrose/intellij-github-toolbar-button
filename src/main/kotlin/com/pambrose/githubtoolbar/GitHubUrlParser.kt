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
 * Converts a Git remote URL into the browsable GitHub page for that repository.
 *
 * Deliberately free of IntelliJ Platform dependencies so it can be unit tested directly.
 */
object GitHubUrlParser {
    private const val HOST = "github.com"

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /** `[user@]host:path` — the scp-style form Git accepts for SSH remotes. */
    private val SCP_STYLE = Regex("^(?:[^@/]+@)?([^:/]+):(.+)$")

    /**
     * Returns `https://github.com/owner/repo`, or `null` if [remoteUrl] is not a well-formed
     * remote pointing at github.com.
     *
     * Any credentials embedded in the remote are discarded rather than carried into the result.
     */
    fun toRepoUrl(remoteUrl: String): String? {
        val trimmed = remoteUrl.trim()
        if (trimmed.isEmpty()) return null

        val (host, path) = splitHostAndPath(trimmed) ?: return null
        if (!isGitHubHost(host)) return null

        val (owner, repo) = splitOwnerAndRepo(path) ?: return null
        return "https://$HOST/$owner/$repo"
    }

    private fun splitHostAndPath(remote: String): Pair<String, String>? =
        when {
            SCHEME.containsMatchIn(remote) -> {
                val rest = remote.substringAfter("://")
                val authority = rest.substringBefore('/')
                // Drop credentials (everything through the last '@') and any port.
                val host = authority.substringAfterLast('@').substringBefore(':')
                host.takeIf { it.isNotEmpty() }?.let { it to rest.substringAfter('/', "") }
            }

            else -> SCP_STYLE.matchEntire(remote)?.let { m ->
                m.groupValues[1] to m.groupValues[2]
            }
        }

    /** Exact match only, so lookalikes such as `evilgithub.com` or `github.com.evil` are rejected. */
    private fun isGitHubHost(host: String): Boolean =
        host.lowercase() in setOf(HOST, "www.$HOST")

    private fun splitOwnerAndRepo(path: String): Pair<String, String>? {
        val segments = path.trim('/').split('/')
        if (segments.size < 2) return null

        val owner = segments[0]
        // Strip the conventional .git suffix, but keep a repository actually named ".git".
        val repo = segments[1].removeSuffix(".git")
        if (owner.isEmpty() || repo.isEmpty()) return null

        return owner to repo
    }
}
