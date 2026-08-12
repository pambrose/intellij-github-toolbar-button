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

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import git4idea.repo.GitRepository

/**
 * Opens a [GitHubBranchDestination] for the repository's current branch.
 *
 * Unlike [OpenGitHubDestinationAction] these need a branch that actually exists on GitHub, so they
 * stay disabled until the current branch tracks an upstream. The tooltip then says whether the
 * problem is a detached HEAD or a branch that has not been pushed, rather than leaving a dead entry
 * with no explanation.
 *
 * Subclasses exist only to bind a destination: the platform instantiates actions reflectively and
 * can call only a no-argument constructor.
 */
abstract class OpenGitHubBranchAction(
    private val destination: GitHubBranchDestination,
) : GitHubUrlAction() {
    override fun urlFor(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String? {
        val repository = GitHubRepoLocator.selectRepository(repositories, contextFile(e)) ?: return null
        return branchUrl(repository)
    }

    override fun describe(url: String): String = "Open $url in the browser"

    override fun perform(url: String) = BrowserUtil.browse(url)

    override fun unavailableReason(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String {
        val repository = GitHubRepoLocator.selectRepository(repositories, contextFile(e))
        // A repository was found and it is on GitHub, so the branch is what is unusable: explain
        // that rather than repeating the generic "no GitHub remote" reason.
        return if (repository != null && GitHubRepoLocator.repoUrlOf(repository, allowedHosts()) != null) {
            GitHubRepoLocator.branchUnavailableReason(repository)
        } else {
            GitHubRepoLocator.unavailableReason(repositories)
        }
    }

    private fun branchUrl(repository: GitRepository): String? {
        val repoUrl = GitHubRepoLocator.repoUrlOf(repository, allowedHosts()) ?: return null
        val branch = GitHubRepoLocator.pushedBranchOf(repository) ?: return null
        return destination.urlFor(repoUrl, branch)
    }
}

class OpenGitHubCurrentBranchAction : OpenGitHubBranchAction(GitHubBranchDestination.BRANCH)

class OpenGitHubNewPullRequestAction : OpenGitHubBranchAction(GitHubBranchDestination.NEW_PULL_REQUEST)
