package com.pambrose.githubtoolbar

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import git4idea.repo.GitRepositoryManager

/**
 * Toolbar button that opens the current project's GitHub repository page in the default browser.
 *
 * The button is present in every project. When no GitHub page can be resolved it stays visible but
 * disabled, and its tooltip explains why.
 */
class OpenOnGitHubAction : AnAction() {

    // Reads Git repository state, which is not allowed on the EDT.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        // Always keep the slot, so the button's position never shifts between projects.
        presentation.isVisible = true

        val project = e.project
        if (project == null) {
            presentation.isEnabled = false
            presentation.description = "No project is open"
            return
        }

        val url = GitHubRepoLocator.locate(project, e.getData(CommonDataKeys.VIRTUAL_FILE))
        presentation.isEnabled = url != null
        presentation.description = url?.let { "Open $it in the browser" }
            ?: GitHubRepoLocator.unavailableReason(GitRepositoryManager.getInstance(project).repositories)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = GitHubRepoLocator.locate(project, e.getData(CommonDataKeys.VIRTUAL_FILE)) ?: return
        BrowserUtil.browse(url)
    }
}
