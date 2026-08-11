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
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

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
) : AnAction() {
    // Reads Git repository state, which is not allowed on the EDT.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project

        if (project == null) {
            presentation.isEnabled = false
            presentation.isVisible = !e.isFromContextMenu
            presentation.description = "No project is open"
            return
        }

        val repository = selectedRepository(e, project)
        val url = repository?.let { branchUrl(it) }

        presentation.isEnabled = url != null
        presentation.isVisible = url != null || !e.isFromContextMenu
        presentation.description = when {
            url != null -> {
                "Open $url in the browser"
            }

            // A repository was found but the branch is unusable, so explain the branch rather than
            // repeating the generic "no GitHub remote" reason.
            repository != null && GitHubRepoLocator.repoUrlOf(repository, allowedHosts()) != null -> {
                GitHubRepoLocator.branchUnavailableReason(repository)
            }

            else -> {
                GitHubRepoLocator.unavailableReason(GitRepositoryManager.getInstance(project).repositories)
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = selectedRepository(e, project) ?: return
        BrowserUtil.browse(branchUrl(repository) ?: return)
    }

    private fun selectedRepository(
        e: AnActionEvent,
        project: Project,
    ): GitRepository? =
        GitHubRepoLocator.selectRepository(
            GitRepositoryManager.getInstance(project).repositories,
            e.getData(CommonDataKeys.VIRTUAL_FILE),
        )

    private fun branchUrl(repository: GitRepository): String? {
        val repoUrl = GitHubRepoLocator.repoUrlOf(repository, allowedHosts()) ?: return null
        val branch = GitHubRepoLocator.pushedBranchOf(repository) ?: return null
        return destination.urlFor(repoUrl, branch)
    }

    private fun allowedHosts(): Set<String> = GitHubHostSettings.getInstance().allowedHosts
}

class OpenGitHubCurrentBranchAction : OpenGitHubBranchAction(GitHubBranchDestination.BRANCH)

class OpenGitHubNewPullRequestAction : OpenGitHubBranchAction(GitHubBranchDestination.NEW_PULL_REQUEST)
