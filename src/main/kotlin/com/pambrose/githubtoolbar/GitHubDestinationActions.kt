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
 * One class per destination, because `plugin.xml` can only instantiate an action through a
 * no-argument constructor. All behaviour lives in [OpenGitHubDestinationAction].
 */

class OpenGitHubPullRequestsAction : OpenGitHubDestinationAction(GitHubDestination.PULL_REQUESTS)

class OpenGitHubIssuesAction : OpenGitHubDestinationAction(GitHubDestination.ISSUES)

class OpenGitHubActionsAction : OpenGitHubDestinationAction(GitHubDestination.ACTIONS)

class OpenGitHubReleasesAction : OpenGitHubDestinationAction(GitHubDestination.RELEASES)
