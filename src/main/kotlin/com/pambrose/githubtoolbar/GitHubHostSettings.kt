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

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * The GitHub Enterprise hosts this user has added, on top of github.com itself.
 *
 * Application-level rather than per-project: an enterprise install belongs to the person, and
 * re-entering it for every project would be busywork.
 */
@State(
    name = "GitHubToolbarButtonSettings",
    storages = [Storage("githubToolbarButton.xml")],
)
class GitHubHostSettings : SimplePersistentStateComponent<GitHubHostSettings.State>(State()) {
    class State : BaseState() {
        /** Stored already normalized by [GitHubUrlParser.normalizeHost]. */
        val hosts: MutableList<String> by list()

        /**
         * Mutation lives here rather than on the service because marking the state dirty uses
         * `BaseState.incrementModificationCount`, which is `protected`. Its public sibling
         * `intIncrementModificationCount` is `@ApiStatus.Internal`, and the Plugin Verifier fails
         * the build over it — so don't reach for that instead.
         */
        fun replaceHosts(normalized: List<String>) {
            if (hosts == normalized) return
            hosts.clear()
            hosts.addAll(normalized)
            incrementModificationCount()
        }
    }

    /**
     * Every host a remote may legitimately use: github.com plus whatever was configured.
     *
     * github.com is always included, so a bad settings entry can only ever fail to add a host —
     * it can never take the default one away.
     */
    val allowedHosts: Set<String>
        get() = GitHubUrlParser.DEFAULT_HOSTS + state.hosts

    /**
     * Replaces the configured hosts, dropping anything that could not be normalized.
     *
     * The reduction itself lives in [GitHubUrlParser.normalizeHosts], because the settings page
     * has to derive the same list to decide whether **Apply** is enabled.
     */
    fun setHosts(raw: List<String>) {
        state.replaceHosts(GitHubUrlParser.normalizeHosts(raw))
    }

    companion object {
        fun getInstance(): GitHubHostSettings = service()
    }
}
