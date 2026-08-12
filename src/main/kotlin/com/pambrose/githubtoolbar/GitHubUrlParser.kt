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

    /** The hosts recognized with no configuration: github.com proper. */
    val DEFAULT_HOSTS: Set<String> = setOf(HOST, "www.$HOST")

    /**
     * Hosts known to be aliases of another host, folded away so both spellings produce one URL.
     * Only github.com's own `www.` alias qualifies — whether some enterprise install answers on
     * `www.` is not ours to guess.
     */
    private val ALIASES: Map<String, String> = mapOf("www.$HOST" to HOST)

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /** `[user@]host:path` — the scp-style form Git accepts for SSH remotes. */
    private val SCP_STYLE = Regex("^(?:[^@/]+@)?([^:/]+):(.+)$")

    /** A plausible hostname: dot-separated labels of letters, digits, and inner dashes. */
    private val HOSTNAME = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$")

    /**
     * Returns `https://host/owner/repo`, or `null` if [remoteUrl] is not a well-formed remote
     * pointing at one of [allowedHosts].
     *
     * Matching against [allowedHosts] is *exact*, which is what rejects lookalikes such as
     * `evilgithub.com` and `github.com.evil.example`. Adding a GitHub Enterprise host therefore
     * widens the set of accepted hosts without weakening how any of them is matched — never
     * "simplify" this into a `contains`/`endsWith`/suffix test.
     *
     * Any credentials embedded in the remote are discarded rather than carried into the result.
     */
    fun toRepoUrl(
        remoteUrl: String,
        allowedHosts: Set<String> = DEFAULT_HOSTS,
    ): String? {
        val trimmed = remoteUrl.trim()
        if (trimmed.isEmpty()) return null

        val (host, path) = splitHostAndPath(trimmed) ?: return null
        val matched = host.lowercase()
        if (allowedHosts.none { it.equals(matched, ignoreCase = true) }) return null

        val (owner, repo) = splitOwnerAndRepo(path) ?: return null
        return "https://${ALIASES[matched] ?: matched}/$owner/$repo"
    }

    /**
     * Reduces whatever a user typed into the settings field to a bare lowercase hostname, or `null`
     * when it could never match a remote anyway.
     *
     * Accepts a bare host, a pasted clone URL, or an scp-style remote, and discards any scheme,
     * credentials, port, and path.
     */
    fun normalizeHost(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val withoutScheme = trimmed.replaceFirst(SCHEME, "")
        val authority = withoutScheme.substringBefore('/')
        val host = authority
            .substringAfterLast('@')
            .substringBefore(':')
            .lowercase()

        return host.takeIf { it.isNotEmpty() && HOSTNAME.matches(it) }
    }

    /**
     * Reduces a block of user-entered lines to exactly what the settings store: bare hostnames,
     * minus anything unusable, minus the defaults (which are unioned in regardless), deduplicated.
     *
     * Single-sourced deliberately, and not merely to save a few lines. The settings page decides
     * whether **Apply** is enabled by comparing what was typed against what was stored, so both
     * sides have to derive the list *identically*. Were one copy to drift stricter, `isModified`
     * would stay true forever after a successful apply; were it to drift looser, a real edit would
     * never enable Apply and the typed host would be discarded in silence.
     */
    fun normalizeHosts(raw: List<String>): List<String> =
        raw
            .mapNotNull(::normalizeHost)
            .filterNot { it in DEFAULT_HOSTS }
            .distinct()

    private fun splitHostAndPath(remote: String): Pair<String, String>? =
        when {
            SCHEME.containsMatchIn(remote) -> {
                val rest = remote.substringAfter("://")
                val authority = rest.substringBefore('/')
                // Drop credentials (everything through the last '@') and any port.
                val host = authority.substringAfterLast('@').substringBefore(':')
                host.takeIf { it.isNotEmpty() }?.let { it to rest.substringAfter('/', "") }
            }

            else -> {
                SCP_STYLE.matchEntire(remote)?.let { m ->
                    m.groupValues[1] to m.groupValues[2]
                }
            }
        }

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
