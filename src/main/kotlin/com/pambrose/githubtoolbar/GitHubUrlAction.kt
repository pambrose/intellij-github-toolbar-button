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

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager

/**
 * Everything the URL-producing actions share: when they are enabled, when they are visible, what
 * their tooltip says, and on which thread that is decided. Subclasses supply only the URL and what
 * to do with it.
 *
 * This exists because the policy below was written out once per action and only explained in one of
 * them — the setup for a change made in two files out of three.
 *
 * Two constraints shape it. Subclasses keep no-argument constructors, because the platform
 * instantiates actions reflectively from `plugin.xml`. And the settings lookup lives here rather
 * than inside [GitHubRepoLocator], so that object still needs no running Application and stays
 * unit-testable against mocked git4idea types alone.
 */
abstract class GitHubUrlAction :
    AnAction(),
    DumbAware {
    // Reads Git repository state, which is not allowed on the EDT.
    final override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    final override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project

        if (project == null) {
            presentation.isEnabled = false
            presentation.isVisible = !e.isFromContextMenu
            presentation.description = "No project is open"
            return
        }

        // Looked up once and handed down, so the disabled path does not repeat the lookup.
        val repositories = repositoriesOf(project)
        val url = urlFor(e, repositories)

        presentation.isEnabled = url != null
        // On a toolbar the button always keeps its slot, so neighbouring icons never shift between
        // projects. In a menu there is no slot worth holding, and a permanently dead entry is just
        // clutter, so it is hidden instead of shown greyed out. (`isFromContextMenu` rather than
        // `ActionPlaces.isPopupPlace`, which the Plugin Verifier flags as deprecated.)
        presentation.isVisible = url != null || !e.isFromContextMenu
        presentation.description = url?.let(::describe) ?: unavailableReason(e, repositories)
    }

    final override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = urlFor(e, repositoriesOf(project)) ?: return
        perform(url)
    }

    /** The URL this action acts on, or `null` when there is none — which is what disables it. */
    protected abstract fun urlFor(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String?

    /** The enabled-state tooltip for a resolved [url]. */
    protected abstract fun describe(url: String): String

    /** What this action does with a resolved [url]. */
    protected abstract fun perform(url: String)

    /**
     * The disabled-state tooltip. Overridden where a more specific reason is available than "no
     * GitHub page could be resolved".
     */
    protected open fun unavailableReason(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String = GitHubRepoLocator.unavailableReason(repositories)

    /** The file the action was invoked on, which decides the Git root in a multi-root project. */
    protected fun contextFile(e: AnActionEvent): VirtualFile? = e.getData(CommonDataKeys.VIRTUAL_FILE)

    protected fun allowedHosts(): Set<String> = GitHubHostSettings.getInstance().allowedHosts

    private fun repositoriesOf(project: Project): List<GitRepository> =
        GitRepositoryManager.getInstance(project).repositories
}
