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
import git4idea.repo.GitRepositoryManager

/**
 * Opens a [GitHubDestination] within the current project's GitHub repository, in the default
 * browser.
 *
 * Subclasses exist only to bind a destination: the platform instantiates actions reflectively from
 * `plugin.xml` and can only call a no-argument constructor, so a destination cannot be passed in as
 * a registration attribute.
 *
 * On a toolbar the action is present in every project — when no GitHub page can be resolved it
 * stays visible but disabled, and its tooltip explains why. In a menu it hides instead, since there
 * is no slot worth holding there.
 */
abstract class OpenGitHubDestinationAction(
    private val destination: GitHubDestination,
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

        val url = destinationUrl(e, project)
        presentation.isEnabled = url != null
        // On a toolbar the button always keeps its slot, so neighbouring icons never shift between
        // projects. In a menu there is no slot worth holding, and a permanently dead entry is just
        // clutter, so it is hidden instead of shown greyed out. (`isFromContextMenu` rather than
        // `ActionPlaces.isPopupPlace`, which the Plugin Verifier flags as deprecated.)
        presentation.isVisible = url != null || !e.isFromContextMenu
        presentation.description = url?.let { "Open $it in the browser" }
            ?: GitHubRepoLocator.unavailableReason(GitRepositoryManager.getInstance(project).repositories)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = destinationUrl(e, project) ?: return
        BrowserUtil.browse(url)
    }

    // The one place that couples resolution to user settings, so GitHubRepoLocator itself needs no
    // running Application and stays testable against mocked git4idea types alone.
    private fun destinationUrl(
        e: AnActionEvent,
        project: Project,
    ): String? =
        GitHubRepoLocator
            .locate(project, e.getData(CommonDataKeys.VIRTUAL_FILE), GitHubHostSettings.getInstance().allowedHosts)
            ?.let(destination::urlFor)
}
