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
    ): String? {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        val repository = selectRepository(repositories, contextFile) ?: return null
        return repoUrlOf(repository, allowedHosts)
    }

    /** Resolves the GitHub page for a single repository, preferring the `origin` remote. */
    fun repoUrlOf(
        repository: GitRepository,
        allowedHosts: Set<String> = GitHubUrlParser.DEFAULT_HOSTS,
    ): String? {
        val remotes = repository.remotes
        val origin = remotes.firstOrNull { it.name == ORIGIN }
        return origin?.gitHubUrl(allowedHosts) ?: remotes.firstNotNullOfOrNull { it.gitHubUrl(allowedHosts) }
    }

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

    /**
     * The current branch of [repository], but only once it tracks an upstream.
     *
     * A branch with no upstream has never been pushed, so every branch-specific URL for it would
     * 404. Returning `null` here is what keeps those actions disabled rather than opening a dead
     * page. `null` also covers a detached HEAD, which git4idea reports as no current branch.
     */
    fun pushedBranchOf(repository: GitRepository): String? {
        val branch = repository.currentBranch ?: return null
        return branch.name.takeIf { repository.getBranchTrackInfo(it) != null }
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
