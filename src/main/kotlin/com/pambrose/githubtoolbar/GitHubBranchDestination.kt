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

/**
 * A page within a GitHub repository that is specific to one branch.
 *
 * Separate from [GitHubDestination] because these need a branch and that one does not. As there, the
 * repository URL is taken as already validated by [GitHubUrlParser] and is never re-parsed, so the
 * exact-host and credential-stripping guarantees still hold.
 *
 * Deliberately free of IntelliJ Platform dependencies so it can be unit tested directly.
 */
enum class GitHubBranchDestination(private val segment: String) {
    BRANCH("tree"),
    NEW_PULL_REQUEST("pull/new"),
    ;

    /**
     * Returns the URL of this destination for [branch] within [repoUrl], or `null` when [branch] is
     * blank.
     */
    fun urlFor(repoUrl: String, branch: String): String? {
        if (branch.isBlank()) return null
        return "${repoUrl.trimEnd('/')}/$segment/${encodePath(branch)}"
    }

    /**
     * Percent-encodes a branch name for use as URL path segments.
     *
     * `/` is left intact: it is ordinary in a branch name and is also the separator GitHub expects,
     * so `feature/x` has to stay `feature/x` rather than becoming `feature%2Fx`. Everything outside
     * the unreserved set is encoded, because Git allows characters in a ref name — `#` and `?` among
     * them — that would otherwise change where the URL ends.
     */
    private fun encodePath(branch: String): String =
        buildString {
            for (byte in branch.toByteArray(Charsets.UTF_8)) {
                val char = byte.toInt().toChar()
                if (byte >= 0 && (char.isLetterOrDigit() && char.code < 128 || char in "-._~/")) {
                    append(char)
                } else {
                    append('%').append("%02X".format(byte.toInt() and 0xFF))
                }
            }
        }
}
