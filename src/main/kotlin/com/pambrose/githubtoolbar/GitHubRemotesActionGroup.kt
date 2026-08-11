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
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import git4idea.repo.GitRepositoryManager

/**
 * Lists every GitHub remote of the current repository, so a fork can reach its upstream.
 *
 * Everywhere else the plugin resolves a single URL, preferring `origin`. That is the right answer
 * almost always, but in a fork it makes `upstream` unreachable — hence this group. It hides itself
 * unless there is genuinely a choice to make, so the common single-remote project sees nothing.
 *
 * The children are built per invocation rather than registered in `plugin.xml`, because the remotes
 * are not known until a project is open.
 */
class GitHubRemotesActionGroup :
    ActionGroup(),
    DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        // One remote is what `Open Repository on GitHub` already does; showing a submenu to choose
        // from a single entry would be noise.
        e.presentation.isVisible = remotes(e).size > 1
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> =
        remotes(e)
            .map { remote ->
                object : AnAction("Open '${remote.name}' on GitHub", "Open ${remote.url} in the browser", null) {
                    override fun actionPerformed(event: AnActionEvent) = BrowserUtil.browse(remote.url)
                }
            }.toTypedArray()

    private fun remotes(e: AnActionEvent?): List<GitHubRepoLocator.NamedRemote> {
        val project = e?.project ?: return emptyList()
        val repository =
            GitHubRepoLocator.selectRepository(
                GitRepositoryManager.getInstance(project).repositories,
                e.getData(CommonDataKeys.VIRTUAL_FILE),
            ) ?: return emptyList()
        return GitHubRepoLocator.gitHubRemotes(repository, GitHubHostSettings.getInstance().allowedHosts)
    }
}
