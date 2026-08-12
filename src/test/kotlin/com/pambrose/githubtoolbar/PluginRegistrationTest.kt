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
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
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
    /** The `class` attribute of every `<action>` and `<group>` in `plugin.xml`. */
    private val registeredClasses: List<String> =
        javaClass.getResourceAsStream("/META-INF/plugin.xml").use { stream ->
            val document =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(checkNotNull(stream) { "META-INF/plugin.xml is not on the test classpath" })

            listOf("action", "group").flatMap { tag ->
                val nodes = document.getElementsByTagName(tag)
                (0 until nodes.length).mapNotNull { index ->
                    (nodes.item(index) as Element).getAttribute("class").takeIf { it.isNotEmpty() }
                }
            }
        }

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
    }
}
