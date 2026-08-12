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

import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitLocalBranch
import git4idea.repo.GitBranchTrackInfo
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GitHubRepoLocatorTest : StringSpec() {
    private fun remote(
        remoteName: String,
        vararg remoteUrls: String,
    ): GitRemote =
        mockk<GitRemote>().also {
            every { it.name } returns remoteName
            every { it.urls } returns remoteUrls.toList()
        }

    private fun repo(
        rootPath: String = "/project",
        vararg remotes: GitRemote,
    ): GitRepository {
        val rootFile = mockk<VirtualFile>().also { every { it.path } returns rootPath }
        return mockk<GitRepository>().also {
            every { it.root } returns rootFile
            every { it.remotes } returns remotes.toList()
        }
    }

    private fun fileAt(filePath: String): VirtualFile = mockk<VirtualFile>().also { every { it.path } returns filePath }

    /**
     * A repository sitting on [branchName], tracking an upstream when [pushed]. Detached HEAD is
     * modelled by a null branch, which is what git4idea reports.
     *
     * [upstreamName] is the name the tracked branch carries on the server, which defaults to the
     * local one but need not match it — `git checkout -b fix origin/main` leaves them different.
     */
    private fun repoOnBranch(
        branchName: String?,
        pushed: Boolean,
        upstreamName: String? = branchName,
    ): GitRepository {
        val repository = repo("/project", remote("origin", "https://github.com/owner/repo.git"))
        val branch = branchName?.let { name ->
            mockk<GitLocalBranch>().also { every { it.name } returns name }
        }
        every { repository.currentBranch } returns branch
        every { repository.getBranchTrackInfo(any()) } returns
            if (pushed && branchName != null) {
                mockk<GitBranchTrackInfo> {
                    every { remoteBranch } returns
                        mockk { every { nameForRemoteOperations } returns upstreamName.orEmpty() }
                }
            } else {
                null
            }
        return repository
    }

    init {
        "prefers the origin remote over other GitHub remotes" {
            val repository = repo(
                "/project",
                remote("upstream", "https://github.com/upstream-owner/repo.git"),
                remote("origin", "https://github.com/my-owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(repository) shouldBe "https://github.com/my-owner/repo"
        }

        "falls back to another GitHub remote when origin is not on GitHub" {
            val repository = repo(
                "/project",
                remote("origin", "https://gitlab.com/my-owner/repo.git"),
                remote("upstream", "https://github.com/upstream-owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(repository) shouldBe "https://github.com/upstream-owner/repo"
        }

        // The fallback scans in declared order, so a GitHub remote declared *before* a non-GitHub
        // origin still wins. This is the case that separates "prefer origin" from "sort origin
        // first and take the head", and the two must agree on it.
        "falls back to a GitHub remote declared before a non-GitHub origin" {
            val repository = repo(
                "/project",
                remote("mirror", "https://github.com/owner/mirror.git"),
                remote("origin", "https://gitlab.com/owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(repository) shouldBe "https://github.com/owner/mirror"
        }

        "uses the only remote when there is no origin" {
            val repository = repo("/project", remote("fork", "git@github.com:someone/repo.git"))

            GitHubRepoLocator.repoUrlOf(repository) shouldBe "https://github.com/someone/repo"
        }

        "checks every url configured on a remote" {
            val repository = repo(
                "/project",
                remote("origin", "https://example.com/mirror.git", "https://github.com/owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(repository) shouldBe "https://github.com/owner/repo"
        }

        "returns null when the repository has no remotes" {
            GitHubRepoLocator.repoUrlOf(repo("/project")) shouldBe null
        }

        "returns null when no remote points at GitHub" {
            val repository = repo(
                "/project",
                remote("origin", "https://gitlab.com/owner/repo.git"),
                remote("backup", "https://bitbucket.org/owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(repository) shouldBe null
        }

        "picks the repository whose root contains the context file" {
            val alpha = repo("/code/alpha", remote("origin", "https://github.com/owner/alpha.git"))
            val beta = repo("/code/beta", remote("origin", "https://github.com/owner/beta.git"))

            val selected = GitHubRepoLocator.selectRepository(listOf(alpha, beta), fileAt("/code/beta/src/Main.kt"))

            selected shouldBe beta
        }

        // The action is registered into ProjectViewPopupMenu, so right-clicking a root hands over
        // that directory itself: its path equals the root's rather than sitting below it. Reduce
        // `isUnder` to a `startsWith("$root/")` test and this silently falls back to the first
        // repository, opening the wrong project's page with every other test still green.
        "picks the repository whose root is the context file" {
            val alpha = repo("/code/alpha", remote("origin", "https://github.com/owner/alpha.git"))
            val beta = repo("/code/beta", remote("origin", "https://github.com/owner/beta.git"))

            GitHubRepoLocator.selectRepository(listOf(alpha, beta), fileAt("/code/beta")) shouldBe beta
        }

        "picks the innermost repository when roots are nested" {
            val outer = repo("/code/outer", remote("origin", "https://github.com/owner/outer.git"))
            val inner = repo("/code/outer/inner", remote("origin", "https://github.com/owner/inner.git"))

            val selected =
                GitHubRepoLocator.selectRepository(listOf(outer, inner), fileAt("/code/outer/inner/src/Main.kt"))

            selected shouldBe inner
        }

        "does not treat a sibling directory with a shared prefix as a match" {
            val alpha = repo("/code/alpha", remote("origin", "https://github.com/owner/alpha.git"))
            val alphaTwo = repo("/code/alpha-two", remote("origin", "https://github.com/owner/alpha-two.git"))

            val selected =
                GitHubRepoLocator.selectRepository(listOf(alpha, alphaTwo), fileAt("/code/alpha-two/src/Main.kt"))

            selected shouldBe alphaTwo
        }

        "falls back to the first repository when the context file is outside every root" {
            val alpha = repo("/code/alpha", remote("origin", "https://github.com/owner/alpha.git"))
            val beta = repo("/code/beta", remote("origin", "https://github.com/owner/beta.git"))

            val selected = GitHubRepoLocator.selectRepository(listOf(alpha, beta), fileAt("/elsewhere/Main.kt"))

            selected shouldBe alpha
        }

        "falls back to the first repository when there is no context file" {
            val alpha = repo("/code/alpha", remote("origin", "https://github.com/owner/alpha.git"))
            val beta = repo("/code/beta", remote("origin", "https://github.com/owner/beta.git"))

            GitHubRepoLocator.selectRepository(listOf(alpha, beta), null) shouldBe alpha
        }

        "returns null when there are no repositories at all" {
            GitHubRepoLocator.selectRepository(emptyList(), null) shouldBe null
        }

        "explains that the project has no Git repository" {
            GitHubRepoLocator.unavailableReason(emptyList()) shouldBe
                "This project has no Git repository"
        }

        "explains that the repository has no remotes" {
            GitHubRepoLocator.unavailableReason(listOf(repo("/project"))) shouldBe
                "This Git repository has no remotes"
        }

        "explains that no remote is on GitHub" {
            val repository = repo("/project", remote("origin", "https://gitlab.com/owner/repo.git"))

            GitHubRepoLocator.unavailableReason(listOf(repository)) shouldBe
                "No github.com remote found"
        }

        // `all`, not `any`: a multi-root project can hold a bare root beside a remoted one, and
        // "no remotes" would then be a lie that sends the user to `git remote add` when the real
        // problem is the host. With a single repository the two are indistinguishable.
        "explains the GitHub reason when only some repositories lack remotes" {
            val bare = repo("/code/bare")
            val gitlab = repo("/code/other", remote("origin", "https://gitlab.com/owner/repo.git"))

            GitHubRepoLocator.unavailableReason(listOf(bare, gitlab)) shouldBe "No github.com remote found"
        }

        // ---- GitHub Enterprise hosts, as supplied by GitHubHostSettings in production ----

        "an enterprise remote resolves once its host is allowed" {
            val repository = repo("/project", remote("origin", "git@github.mycompany.com:owner/repo.git"))

            GitHubRepoLocator.repoUrlOf(repository) shouldBe null
            GitHubRepoLocator.repoUrlOf(repository, setOf("github.mycompany.com")) shouldBe
                "https://github.mycompany.com/owner/repo"
        }

        "origin still wins over another allowed enterprise remote" {
            val repository = repo(
                "/project",
                remote("upstream", "https://github.mycompany.com/upstream/repo.git"),
                remote("origin", "https://github.com/owner/repo.git"),
            )

            GitHubRepoLocator.repoUrlOf(
                repository,
                GitHubUrlParser.DEFAULT_HOSTS + "github.mycompany.com",
            ) shouldBe "https://github.com/owner/repo"
        }

        "a remote on an unlisted host stays unresolved even with other hosts allowed" {
            val repository = repo("/project", remote("origin", "https://github.elsewhere.com/owner/repo.git"))

            GitHubRepoLocator.repoUrlOf(repository, setOf("github.mycompany.com")) shouldBe null
        }
        // ---- Current branch, for the branch-specific destinations ----

        "reports the current branch once it tracks an upstream" {
            GitHubRepoLocator.pushedBranchOf(repoOnBranch("feature/x", pushed = true)) shouldBe "feature/x"
        }

        // `git checkout -b fix origin/main` tracks an upstream under a different name, and only the
        // upstream's name exists on GitHub — `/tree/fix` would 404.
        "reports the upstream's name when it differs from the local one" {
            GitHubRepoLocator.pushedBranchOf(
                repoOnBranch("fix", pushed = true, upstreamName = "main"),
            ) shouldBe "main"
        }

        // A branch with no upstream has never been pushed, so every branch URL for it would 404.
        "reports no branch when the current one has not been pushed" {
            GitHubRepoLocator.pushedBranchOf(repoOnBranch("feature/x", pushed = false)) shouldBe null
        }

        "reports no branch on a detached HEAD" {
            GitHubRepoLocator.pushedBranchOf(repoOnBranch(null, pushed = false)) shouldBe null
        }

        "explains a detached HEAD" {
            GitHubRepoLocator.branchUnavailableReason(repoOnBranch(null, pushed = false)) shouldBe
                "Not currently on a branch"
        }

        "explains an unpushed branch by name" {
            GitHubRepoLocator.branchUnavailableReason(repoOnBranch("feature/x", pushed = false)) shouldBe
                "Branch 'feature/x' has not been pushed"
        }

        // ---- Every GitHub remote, for choosing between them ----

        "lists a single GitHub remote" {
            val repository = repo("/project", remote("origin", "https://github.com/owner/repo.git"))

            GitHubRepoLocator.gitHubRemotes(repository) shouldBe
                listOf(GitHubRepoLocator.NamedRemote("origin", "https://github.com/owner/repo"))
        }

        // A fork has both, and origin winning silently is exactly what makes upstream unreachable.
        "lists every GitHub remote with origin first" {
            val repository = repo(
                "/project",
                remote("upstream", "https://github.com/upstream-owner/repo.git"),
                remote("origin", "https://github.com/my-owner/repo.git"),
            )

            GitHubRepoLocator.gitHubRemotes(repository) shouldBe
                listOf(
                    GitHubRepoLocator.NamedRemote("origin", "https://github.com/my-owner/repo"),
                    GitHubRepoLocator.NamedRemote("upstream", "https://github.com/upstream-owner/repo"),
                )
        }

        "keeps the declared order among the non-origin remotes" {
            val repository = repo(
                "/project",
                remote("bravo", "https://github.com/owner/bravo.git"),
                remote("alpha", "https://github.com/owner/alpha.git"),
            )

            GitHubRepoLocator.gitHubRemotes(repository).map { it.name } shouldBe listOf("bravo", "alpha")
        }

        "leaves out remotes that are not on an allowed host" {
            val repository = repo(
                "/project",
                remote("origin", "https://github.com/owner/repo.git"),
                remote("mirror", "https://gitlab.com/owner/repo.git"),
            )

            GitHubRepoLocator.gitHubRemotes(repository).map { it.name } shouldBe listOf("origin")
        }

        "lists an enterprise remote once its host is allowed" {
            val repository = repo(
                "/project",
                remote("origin", "https://github.com/owner/repo.git"),
                remote("work", "https://github.mycompany.com/owner/repo.git"),
            )

            GitHubRepoLocator.gitHubRemotes(repository).map { it.name } shouldBe listOf("origin")
            GitHubRepoLocator.gitHubRemotes(
                repository,
                GitHubUrlParser.DEFAULT_HOSTS + "github.mycompany.com",
            ).map { it.name } shouldBe listOf("origin", "work")
        }

        "returns nothing when no remote is on GitHub" {
            val repository = repo("/project", remote("origin", "https://gitlab.com/owner/repo.git"))

            GitHubRepoLocator.gitHubRemotes(repository) shouldBe emptyList()
        }
    }
}
