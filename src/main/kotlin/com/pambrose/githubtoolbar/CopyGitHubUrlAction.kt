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
import com.intellij.openapi.ide.CopyPasteManager
import git4idea.repo.GitRepositoryManager

/**
 * Copies the repository's GitHub URL instead of opening it, for pasting into a review, a chat
 * message, or a commit body.
 *
 * Resolution is identical to [OpenOnGitHubAction], so the copied URL is always the page the button
 * would have opened.
 */
class CopyGitHubUrlAction : AnAction() {
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

        val url = repositoryUrl(e)
        presentation.isEnabled = url != null
        presentation.isVisible = url != null || !e.isFromContextMenu
        presentation.description =
            url?.let { "Copy $it to the clipboard" }
                ?: GitHubRepoLocator.unavailableReason(GitRepositoryManager.getInstance(project).repositories)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val url = repositoryUrl(e) ?: return
        CopyPasteManager.copyTextToClipboard(url)
    }

    private fun repositoryUrl(e: AnActionEvent): String? {
        val project = e.project ?: return null
        return GitHubRepoLocator.locate(
            project,
            e.getData(CommonDataKeys.VIRTUAL_FILE),
            GitHubHostSettings.getInstance().allowedHosts,
        )
    }
}
