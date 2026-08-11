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

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings → Tools → GitHub Toolbar Button.
 *
 * One host per line, kept as free text rather than a table: the list is short, and pasting a clone
 * URL is the likely way a user knows their enterprise host. [GitHubUrlParser.normalizeHost] reduces
 * whatever they type to a bare hostname, and anything it cannot make sense of is dropped on apply,
 * which the preview label makes visible before they commit to it.
 */
class GitHubHostConfigurable : Configurable {
    private val hostsField = JBTextArea(6, 40)
    private val preview = JBLabel()

    override fun getDisplayName(): String = "GitHub Toolbar Button"

    override fun createComponent(): JComponent {
        hostsField.emptyText.text = "github.mycompany.com"
        hostsField.document.addUndoableEditListener { updatePreview() }

        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("GitHub Enterprise hosts:"), hostsField, true)
            .addComponentToRightColumn(
                JBLabel(
                    "<html>One host per line. A full clone URL works too — only its host is kept.<br>" +
                        "github.com is always recognized and does not need to be listed.</html>",
                ).apply { border = JBUI.Borders.emptyTop(4) },
            )
            .addComponentToRightColumn(preview)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            .also { updatePreview() }
    }

    override fun isModified(): Boolean = enteredHosts() != storedHosts()

    override fun apply() {
        GitHubHostSettings.getInstance().setHosts(hostsField.text.lines())
        reset()
    }

    override fun reset() {
        hostsField.text = storedHosts().joinToString("\n")
        updatePreview()
    }

    private fun storedHosts(): List<String> = GitHubHostSettings.getInstance().state.hosts.toList()

    private fun enteredHosts(): List<String> =
        hostsField.text.lines()
            .mapNotNull(GitHubUrlParser::normalizeHost)
            .filterNot { it in GitHubUrlParser.DEFAULT_HOSTS }
            .distinct()

    /** Shows what will actually be stored, so a typo is visible before it is saved and forgotten. */
    private fun updatePreview() {
        val hosts = enteredHosts()
        preview.text = when {
            hostsField.text.isBlank() -> "Only github.com is recognized."
            hosts.isEmpty() -> "No usable host recognized — only github.com will be used."
            else -> "Will recognize: " + (GitHubUrlParser.DEFAULT_HOSTS + hosts).joinToString(", ")
        }
    }
}
