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

    /** Resolves the GitHub page for [project], or `null` if there is none to open. */
    fun locate(project: Project, contextFile: VirtualFile?): String? {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        val repository = selectRepository(repositories, contextFile) ?: return null
        return repoUrlOf(repository)
    }

    /** Resolves the GitHub page for a single repository, preferring the `origin` remote. */
    fun repoUrlOf(repository: GitRepository): String? {
        val remotes = repository.remotes
        val origin = remotes.firstOrNull { it.name == ORIGIN }
        return origin?.gitHubUrl() ?: remotes.firstNotNullOfOrNull { it.gitHubUrl() }
    }

    /** Picks the repository owning [contextFile], falling back to the first one. */
    fun selectRepository(repositories: List<GitRepository>, contextFile: VirtualFile?): GitRepository? {
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

    /** Explains, for the disabled button's tooltip, why no GitHub page could be resolved. */
    fun unavailableReason(repositories: List<GitRepository>): String =
        when {
            repositories.isEmpty() -> "This project has no Git repository"
            repositories.all { it.remotes.isEmpty() } -> "This Git repository has no remotes"
            else -> "No github.com remote found"
        }

    private fun GitRemote.gitHubUrl(): String? = urls.firstNotNullOfOrNull(GitHubUrlParser::toRepoUrl)

    /** Compares whole path segments, so `/code/alpha-two` is not treated as being under `/code/alpha`. */
    private fun String.isUnder(root: String): Boolean = this == root || startsWith("$root/")
}
