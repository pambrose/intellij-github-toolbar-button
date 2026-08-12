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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import git4idea.repo.GitRepository

/**
 * Copies the repository's GitHub URL instead of opening it, for pasting into a review, a chat
 * message, or a commit body.
 *
 * Resolution is identical to [OpenOnGitHubAction] — both go through [GitHubUrlAction] — so the
 * copied URL is always the page the button would have opened.
 */
class CopyGitHubUrlAction : GitHubUrlAction() {
    override fun urlFor(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String? = GitHubRepoLocator.locate(repositories, contextFile(e), allowedHosts())

    override fun describe(url: String): String = "Copy $url to the clipboard"

    override fun perform(url: String) = CopyPasteManager.copyTextToClipboard(url)
}
