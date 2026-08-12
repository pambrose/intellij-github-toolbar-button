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

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

/**
 * The **GitHub** submenu.
 *
 * Every child hides itself in a context menu when there is no GitHub page to open, so that a
 * permanently dead entry is not left behind. Without this class the submenu holding them would
 * undo that: the platform's default for an emptied popup group is to grey it out and keep it in
 * the menu (`isDisableGroupIfEmpty` is on by default, `isHideGroupIfEmpty` is not), leaving exactly
 * the dead entry the children took care to avoid.
 *
 * The flag is set on the template presentation from here rather than by overriding
 * `createTemplatePresentation()`, the way `DefaultCompactActionGroup` does it. That method is
 * `@ApiStatus.Internal` and **fails `make verify`** — the same trap as `intIncrementModificationCount`
 * in [GitHubHostSettings]. Note also that `compact="true"` in `plugin.xml` is not a substitute: it
 * only hides *disabled* children and still leaves the emptied group visible.
 *
 * [DumbAware] because nothing below this group reads an index, and a group that is not dumb-aware
 * is greyed out for the whole of indexing.
 */
class GitHubDestinationsGroup :
    DefaultActionGroup(),
    DumbAware {
    init {
        templatePresentation.isHideGroupIfEmpty = true
    }
}
