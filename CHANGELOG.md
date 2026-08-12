# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Published artifacts are on the
[releases page](https://github.com/pambrose/intellij-github-toolbar-button/releases).

## [Unreleased]

### Fixed

- **Open Current Branch on GitHub** and **Open Pull Request for Current Branch** now open the name
  the branch carries on its remote rather than its local name. The two differ whenever a branch was
  created from, or pushed to, a differently named upstream — `git checkout -b fix origin/main`, or
  `git push -u origin HEAD:release-2026` — and in those cases both actions were enabled and then
  opened a page that does not exist.
- Every command now works while the IDE is building its indexes. None of them reads an index, but
  none was marked as such, so the platform refused them for the whole of indexing — from the VCS
  Operations popup, the Project View context menu, a keyboard shortcut, or Find Action — while still
  showing them as enabled. The toolbar button was unaffected, which is why the two disagreed right
  after opening a project or switching branches.
- The **GitHub** submenu now hides itself in a context menu when it has nothing to offer, instead of
  appearing greyed out. Its entries already hid themselves individually; the submenu holding them
  did not, so an empty **GitHub ▸** was left behind in projects with no GitHub remote.

## [1.1.0] - 2026-08-11

### Added

- A **GitHub** submenu in the VCS Operations popup and the Project View context menu, opening the
  repository's **Pull Requests**, **Issues**, **Actions**, and **Releases** alongside its home page.
  Each is a separate command, so any of them can be bound to a keyboard shortcut.
- **Open Pull Request for Current Branch**, which opens GitHub's create-PR page for the branch you
  are on, and **Open Current Branch on GitHub**, which opens its tree view. Both stay disabled until
  the current branch tracks an upstream, because a branch that has never been pushed has no page on
  GitHub; the tooltip distinguishes a detached HEAD from an unpushed branch rather than leaving a
  dead entry unexplained. Branch names are percent-encoded for the URL, except for `/`, which stays
  a path separator so `feature/x` resolves correctly.
- **Copy GitHub URL**, which puts the repository URL on the clipboard instead of opening a browser.
  It resolves the URL exactly as the toolbar button does.
- An **Open Other Remote** submenu listing every GitHub remote, so a fork can reach its `upstream`
  rather than always following `origin`. It appears only when a repository has more than one GitHub
  remote, so single-remote projects see nothing new.
- GitHub Enterprise support. Additional hosts are configured under **Settings | Tools | GitHub
  Toolbar Button**, one per line; a pasted clone URL is reduced to its host. Configured hosts are
  matched exactly, exactly as `github.com` always was, so adding `github.mycompany.com` recognizes
  that host alone and not `evilgithub.mycompany.com` or `github.mycompany.com.evil.example`.
  `github.com` remains recognized regardless of what is configured.
- Enterprise repository URLs keep their own host, rather than being rewritten to `github.com`.

### Changed

- The command is now named **Open Repository on GitHub**. The bundled GitHub plugin already presents
  its own, different action as "Open on GitHub" in Find Action — that one opens the current *file* at
  the current revision.
- In a context menu the command hides when no GitHub page can be resolved, instead of appearing
  permanently greyed out. On a toolbar it still stays visible but disabled, so the button never
  vacates its slot and neighbouring icons never shift between projects.
- README version badges are now generated from `gradle/libs.versions.toml`, so they can no longer
  fall out of date with the build.

## [1.0.0] - 2026-08-11

First release.

### Added

- A main-toolbar button that opens the current project's GitHub repository home page in the default
  browser — `https://github.com/owner/repo`, not a branch or file URL, so it works even when the
  current branch has never been pushed.
- The button occupies its toolbar slot in every project, in both the new and classic UI, so its
  position never shifts. Where no GitHub page can be resolved it is disabled and its tooltip
  distinguishes between having no Git repository, no remote, and no GitHub remote.
- Recognition of every common remote form: `https://`, scp-style `git@github.com:owner/repo.git`,
  `ssh://`, and `git://`, with or without a `.git` suffix, and with an SSH port present.
- Credentials embedded in a remote URL (`https://user:token@github.com/...`) are stripped before the
  URL reaches the browser.
- Exact host matching against `github.com` / `www.github.com`, which rejects lookalike hosts such as
  `evilgithub.com` and `github.com.evil.example`.
- Remote selection prefers `origin`, falling back to the first remote that resolves to GitHub.
  Repository selection prefers the Git root owning the current file — innermost first where roots
  nest — falling back to the project's first root. Root containment compares whole path segments, so
  `/code/alpha` does not swallow `/code/alpha-two`.
- Apache License 2.0.
- CI covering build, tests, and the IntelliJ Plugin Verifier, plus tag-driven release automation that
  refuses to publish when the tag disagrees with the version in `gradle.properties`.

[Unreleased]: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.1.0...HEAD
[1.1.0]: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.0.0...1.1.0
[1.0.0]: https://github.com/pambrose/intellij-github-toolbar-button/commits/1.0.0
