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
 * Opens a [GitHubDestination] within the current project's GitHub repository, in the default
 * browser.
 *
 * Subclasses exist only to bind a destination: the platform instantiates actions reflectively from
 * `plugin.xml` and can only call a no-argument constructor, so a destination cannot be passed in as
 * a registration attribute.
 *
 * When it is enabled, when it is visible and what its tooltip says all come from [GitHubUrlAction].
 */
abstract class OpenGitHubDestinationAction(
    private val destination: GitHubDestination,
) : GitHubUrlAction() {
    override fun urlFor(
        e: AnActionEvent,
        repositories: List<GitRepository>,
    ): String? = GitHubRepoLocator.locate(repositories, contextFile(e), allowedHosts())?.let(destination::urlFor)

    override fun describe(url: String): String = "Open $url in the browser"

    override fun perform(url: String) = BrowserUtil.browse(url)
}
