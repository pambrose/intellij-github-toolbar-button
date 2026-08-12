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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

/**
 * Resolves the GitHub page for a project, given the Git repositories the IDE already tracks.
 *
 * Selection rules:
 * - among several Git roots, the root containing the current file wins (innermost, when nested),
 *   otherwise the project's first root;
 * - among several remotes, `origin` wins, otherwise the first remote resolving to a GitHub URL.
 */
object GitHubRepoLocator {
    private const val ORIGIN = "origin"

    /**
     * Resolves the GitHub page for [project], or `null` if there is none to open.
     *
     * [allowedHosts] is passed in rather than read from [GitHubHostSettings] here, so this object
     * needs no running Application and stays unit-testable with nothing but mocked git4idea types.
     * Production callers pass the configured set; the default covers github.com only.
     */
    fun locate(
        project: Project,
        contextFile: VirtualFile?,
        allowedHosts: Set<String> = GitHubUrlParser.DEFAULT_HOSTS,
    ): String? = locate(GitRepositoryManager.getInstance(project).repositories, contextFile, allowedHosts)

    /**
     * As [locate], for a caller that already holds the project's repositories — which the action
     * layer does, having needed them for the disabled-state tooltip anyway.
     *
     * Unlike the [Project] overload this one touches no platform service, so it is directly
     * unit-testable against mocked git4idea types.
     */
    fun locate(
        repositories: List<GitRepository>,
        contextFile: VirtualFile?,
        allowedHosts: Set<String> = GitHubUrlParser.DEFAULT_HOSTS,
    ): String? = selectRepository(repositories, contextFile)?.let { repoUrlOf(it, allowedHosts) }

    /**
     * Resolves the GitHub page for a single repository, preferring the `origin` remote.
     *
     * Expressed through [gitHubRemotes] so that the origin-first preference is written down once.
     * The URL this returns is, by construction, the first entry the remotes submenu offers, and
     * the two cannot drift into disagreeing about where a repository lives. It does mean every
     * remote is parsed rather than stopping at `origin`; for the one to three remotes a real
     * repository has, that is not worth a second copy of the rule.
     */
    fun repoUrlOf(
        repository: GitRepository,
        allowedHosts: Set<String> = GitHubUrlParser.DEFAULT_HOSTS,
    ): String? = gitHubRemotes(repository, allowedHosts).firstOrNull()?.url

    /** Picks the repository owning [contextFile], falling back to the first one. */
    fun selectRepository(
        repositories: List<GitRepository>,
        contextFile: VirtualFile?,
    ): GitRepository? {
        if (repositories.isEmpty()) return null

        val path = contextFile?.path
        if (path != null) {
            repositories
                .filter { path.isUnder(it.root.path) }
                // Longest matching root is the innermost one, which is the correct owner.
                .maxByOrNull { it.root.path.length }
                ?.let { return it }
        }

        return repositories.first()
    }

    /** A Git remote that resolves to a GitHub page, paired with the name it is configured under. */
    data class NamedRemote(
        val name: String,
        val url: String,
    )

    /**
     * Every remote of [repository] that resolves to a GitHub page, `origin` first and the rest in
     * their configured order.
     *
     * [repoUrlOf] answers "where does this project live", which is the right question almost always.
     * This answers "where could it live", which is what a fork needs: with `origin` and `upstream`
     * both on GitHub, preferring `origin` silently makes the other unreachable.
     */
    fun gitHubRemotes(
        repository: GitRepository,
        allowedHosts: Set<String> = GitHubUrlParser.DEFAULT_HOSTS,
    ): List<NamedRemote> =
        repository.remotes
            .sortedBy { it.name != ORIGIN }
            .mapNotNull { remote ->
                remote.gitHubUrl(allowedHosts)?.let { NamedRemote(remote.name, it) }
            }

    /**
     * The name the current branch of [repository] has on its remote, but only once it tracks one.
     *
     * A branch with no upstream has never been pushed, so every branch-specific URL for it would
     * 404. Returning `null` here is what keeps those actions disabled rather than opening a dead
     * page. `null` also covers a detached HEAD, which git4idea reports as no current branch.
     *
     * The *upstream's* name is returned rather than the local one, because that is the name that
     * exists on the server and the two need not match: `git checkout -b fix origin/main` and
     * `git push -u origin HEAD:release-2026` both leave a local name that would 404 on GitHub.
     */
    fun pushedBranchOf(repository: GitRepository): String? {
        val branch = repository.currentBranch ?: return null
        return repository.getBranchTrackInfo(branch.name)?.remoteBranch?.nameForRemoteOperations
    }

    /** Explains, for a disabled branch action's tooltip, why no branch URL could be built. */
    fun branchUnavailableReason(repository: GitRepository): String {
        val branch = repository.currentBranch ?: return "Not currently on a branch"
        return "Branch '${branch.name}' has not been pushed"
    }

    /** Explains, for the disabled button's tooltip, why no GitHub page could be resolved. */
    fun unavailableReason(repositories: List<GitRepository>): String =
        when {
            repositories.isEmpty() -> "This project has no Git repository"
            repositories.all { it.remotes.isEmpty() } -> "This Git repository has no remotes"
            else -> "No github.com remote found"
        }

    private fun GitRemote.gitHubUrl(allowedHosts: Set<String>): String? =
        urls.firstNotNullOfOrNull { GitHubUrlParser.toRepoUrl(it, allowedHosts) }

    /** Compares whole path segments, so `/code/alpha-two` is not treated as being under `/code/alpha`. */
    private fun String.isUnder(root: String): Boolean = this == root || startsWith("$root/")
}
