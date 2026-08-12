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

import com.intellij.openapi.project.DumbAware
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Checks the `plugin.xml` registrations that the compiler cannot.
 *
 * Every class named there is named as a *string*, and every promise made about those classes —
 * that they exist, that they are usable while the IDE indexes — holds only by convention. None of
 * it needs a running Application to verify, so it belongs in the plain unit suite: classes are
 * loaded with `initialize = false` and only their type hierarchy is inspected.
 */
class PluginRegistrationTest : StringSpec() {
    private val document: Document =
        javaClass.getResourceAsStream("/META-INF/plugin.xml").use { stream ->
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(checkNotNull(stream) { "META-INF/plugin.xml is not on the test classpath" })
        }

    /** The `class` attribute of every `<action>` and `<group>` in `plugin.xml`. */
    private val registeredClasses: List<String> =
        listOf("action", "group").flatMap { tag ->
            elements(tag).mapNotNull { it.getAttribute("class").takeIf(String::isNotEmpty) }
        }

    private fun elements(tag: String): List<Element> =
        document.getElementsByTagName(tag).let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }

    /** The `<group>` registered under [id], which the destination-menu tests below then read. */
    private fun group(id: String): Element =
        checkNotNull(elements("group").singleOrNull { it.getAttribute("id") == id }) {
            "plugin.xml declares no single <group id=\"$id\">"
        }

    private fun childrenOf(
        parent: Element,
        tag: String,
    ): List<Element> = elements(tag).filter { it.parentNode === parent }

    private fun loadWithoutInitializing(className: String): Class<*> =
        Class.forName(className, false, javaClass.classLoader)

    init {
        // Guards the two tests below: were the parse ever to come back empty, both would pass
        // vacuously while checking nothing at all.
        "plugin.xml yields the action and group classes this spec then checks" {
            registeredClasses shouldContain "com.pambrose.githubtoolbar.OpenOnGitHubAction"
            registeredClasses.size shouldBeGreaterThanOrEqual 8
        }

        // A class name in plugin.xml is a string nothing checks at build time, so renaming or
        // moving a class breaks its registration silently — the action simply never appears.
        "every registered class exists under the name plugin.xml gives it" {
            registeredClasses.forEach { className ->
                withClue(className) {
                    loadWithoutInitializing(className).name shouldBe className
                }
            }
        }

        // Nothing in this plugin reads an index. An action that is not dumb-aware is refused for
        // the whole of indexing through the menu, keymap and Find Action paths, while `update()`
        // still presents it as enabled — so it looks available and then does nothing.
        "every registered action and group is dumb-aware" {
            registeredClasses.forEach { className ->
                withClue(className) {
                    DumbAware::class.java.isAssignableFrom(loadWithoutInitializing(className)) shouldBe true
                }
            }
        }

        // A group id that does not exist is not an error — the submenu simply never appears, which
        // is how it went unnoticed that the main menu had been left out entirely. Each id here was
        // read out of the IDE's own descriptors: `Git.MainMenu` from git4idea, the other two from
        // the platform.
        "the destinations submenu is registered into the main menu as well as both popups" {
            val hosts =
                childrenOf(group(DESTINATIONS_MENU), "add-to-group")
                    .map { it.getAttribute("group-id") }

            hosts shouldContainExactlyInAnyOrder
                listOf("Git.MainMenu", "Vcs.Operations.Popup", "ProjectViewPopupMenu")
        }

        // The bundled GitHub plugin puts its own submenu, titled "GitHub", into Git.MainMenu. Drop
        // this override and the Git menu carries two adjacent submenus under the same name, which
        // no user can tell apart.
        "the submenu is retitled in the main menu, where the bundled plugin already holds 'GitHub'" {
            val overrides =
                childrenOf(group(DESTINATIONS_MENU), "override-text")
                    .filter { it.getAttribute("place") == "MainMenu" }

            overrides.map { it.getAttribute("text") } shouldBe listOf("Open on GitHub")
        }
    }

    private companion object {
        const val DESTINATIONS_MENU = "com.pambrose.githubtoolbar.DestinationsMenu"
    }
}
