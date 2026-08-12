# GitHub Toolbar Button

[![Build](https://github.com/pambrose/intellij-github-toolbar-button/actions/workflows/build.yml/badge.svg)](https://github.com/pambrose/intellij-github-toolbar-button/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/pambrose/intellij-github-toolbar-button/master?logo=codecov&logoColor=white&label=coverage&color=F01F7A)](https://codecov.io/gh/pambrose/intellij-github-toolbar-button)
[![Release](https://img.shields.io/github/v/release/pambrose/intellij-github-toolbar-button?logo=github&color=2ea44f)](https://github.com/pambrose/intellij-github-toolbar-button/releases/latest)
[![IntelliJ IDEA](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fpambrose%2Fintellij-github-toolbar-button%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.intellijIdea&suffix=%2B&label=IntelliJ%20IDEA&color=000000&logo=intellijidea&logoColor=white)](https://www.jetbrains.com/idea/)
[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fpambrose%2Fintellij-github-toolbar-button%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=7F52FF&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fpambrose%2Fintellij-github-toolbar-button%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions.jvm&label=JDK&color=437291&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Gradle](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fpambrose%2Fintellij-github-toolbar-button%2Fmaster%2Fgradle%2Flibs.versions.toml&query=%24.versions%5B%27gradle-wrapper%27%5D&label=Gradle&color=02303A&logo=gradle&logoColor=white)](https://gradle.org)
[![License](https://img.shields.io/github/license/pambrose/intellij-github-toolbar-button?color=D22128&logo=apache&logoColor=white)](LICENSE)

An IntelliJ IDEA plugin that adds a button to the main toolbar. Click it, and the project's
GitHub page opens in your default browser.

## What it does

IntelliJ's bundled GitHub integration can open the *current file at the current revision* from a
context menu. This plugin does something simpler and more direct: one always-present toolbar
button that takes you to the repository's home page.

- **Always visible.** The button occupies its toolbar slot in every project, so its position
  never shifts around on you.
- **More than the home page.** A **GitHub** submenu in the VCS Operations popup
  (<kbd>Ctrl</kbd>/<kbd>Cmd</kbd>+<kbd>V</kbd>) and the Project View right-click menu also opens
  **Pull Requests**, **Issues**, **Actions**, and **Releases** for the same repository. Each is a
  separate command, so any of them can take a keyboard shortcut. Right-clicking a file picks the
  Git root that owns it, which matters in a project with nested roots.
- **Copy instead of open.** **Copy GitHub URL** puts the repository URL on the clipboard, for
  pasting into a review or a chat message.
- **Forks can reach upstream.** When a repository has more than one GitHub remote, an **Open Other
  Remote** submenu lists them all; `origin` is still what the button itself follows.
- **Straight from a branch to its pull request.** **Open Pull Request for Current Branch** goes to
  GitHub's create-PR page for the branch you are on, and **Open Current Branch on GitHub** opens its
  tree view. Both stay disabled until the branch tracks an upstream — an unpushed branch has no page
  to open — and the tooltip says whether you are on a detached HEAD or simply have not pushed yet.
- **Disabled when it can't help.** In a project with no Git repository, no remote, or a
  non-GitHub remote, the button greys out and its tooltip explains why. It never fails silently
  and never opens a dialog you didn't ask for. In the menus these commands hide instead of
  appearing permanently greyed out — there is no slot worth holding there.
- **Opens the repository home page** — `https://github.com/owner/repo` — not a branch or file
  URL, so it works even when your current branch has never been pushed.

### Which remote it uses

For a project with several remotes, the `origin` remote wins; otherwise the first remote that
resolves to a GitHub URL is used. For a project with several Git roots, the root owning the
currently-open file wins, falling back to the project's first root.

Every common remote URL form is recognized:

| Remote URL | Opens |
| --- | --- |
| `https://github.com/owner/repo.git` | `https://github.com/owner/repo` |
| `git@github.com:owner/repo.git` | `https://github.com/owner/repo` |
| `ssh://git@github.com/owner/repo.git` | `https://github.com/owner/repo` |
| `git://github.com/owner/repo.git` | `https://github.com/owner/repo` |

Credentials embedded in a remote URL (`https://user:token@github.com/...`) are stripped before
the URL reaches your browser.

### GitHub Enterprise

Only `github.com` is recognized out of the box. For a self-hosted install, add its host under
**Settings → Tools → GitHub Toolbar Button**, one per line — pasting a full clone URL works too,
since only the host is kept. Configured hosts are matched exactly, the same way `github.com` is, so
adding `github.mycompany.com` recognizes that host and nothing else: not `evilgithub.mycompany.com`,
and not `github.mycompany.com.evil.example`. `github.com` stays recognized regardless, so a mistake
in this field can never take the default away.

Enterprise repository URLs keep their own host — `https://github.mycompany.com/owner/repo`.

## Requirements

- IntelliJ IDEA 2025.2 or later (Community or Ultimate)
- The bundled **Git** plugin enabled — it supplies the repository data this plugin reads
- JDK 21, to build from source

## Installation

The plugin is not published to the JetBrains Marketplace, so install it from a locally built
distribution.

**1. Clone and build**

```bash
git clone https://github.com/pambrose/intellij-github-toolbar-button.git
cd intellij-github-toolbar-button
./gradlew buildPlugin
```

This writes a distributable ZIP to `build/distributions/`.

**2. Install into your IDE**

In IntelliJ IDEA, open **Settings → Plugins**, click the **⚙️ gear icon**, choose
**Install Plugin from Disk…**, and select the ZIP from `build/distributions/`. Restart the IDE
when prompted.

### Trying it without installing

To launch a sandboxed IDE with the plugin preloaded — the normal loop while developing — run:

```bash
./gradlew runIde
```

This starts a separate IntelliJ instance with its own settings, leaving your daily IDE
untouched.

## Usage

Open any project backed by a GitHub repository and click the button in the main toolbar. The
repository page opens in your default browser.

If the button is greyed out, hover it: the tooltip states whether the project has no Git
repository, no remote, or a remote that isn't on `github.com`.

The command is named **Open Repository on GitHub** — distinct from the bundled GitHub plugin's
"Open on GitHub", which opens the current *file* at the current revision. Bind it to a keyboard
shortcut under **Settings → Keymap** by searching for that name, and move the toolbar button itself
via **Settings → Appearance & Behavior → Menus and Toolbars → Main Toolbar**.

## Development

A `Makefile` wraps the common tasks — run `make` on its own to list them:

```bash
make build      # compile and test
make tests      # tests only
make run        # launch a sandbox IDE with the plugin
make dist       # produce the installable ZIP
make verify     # plugin compatibility check
make check      # wrapper check, then ktlint + tests + verifier
make lint       # report ktlint violations
make format     # fix what ktlint can fix automatically
make versions   # report dependencies with newer stable releases
make all        # clean, build, package, verify

make test-one TEST=com.pambrose.githubtoolbar.GitHubUrlParserTest
```

These call Gradle underneath, which you can also invoke directly:

```bash
./gradlew build              # compile and test
./gradlew test               # tests only
./gradlew runIde             # launch a sandbox IDE with the plugin
./gradlew buildPlugin        # produce the installable ZIP
./gradlew verifyPlugin       # check compatibility across supported IDE versions
./gradlew lintKotlin         # report ktlint violations
./gradlew formatKotlin       # fix what ktlint can fix automatically
./gradlew dependencyUpdates  # report newer stable dependency versions
```

The Gradle wrapper version is pinned in `gradle/libs.versions.toml`. To move it, bump
`gradle-wrapper` there and run `make upgrade-wrapper`. Since nothing in the build resolves that
entry, `make check-wrapper` guards against the pin and `gradle/wrapper/gradle-wrapper.properties`
drifting apart; `make check` runs it first.

Tests use [Kotest](https://kotest.io/) with [MockK](https://mockk.io/). Run a single test class
with:

```bash
./gradlew test --tests "com.pambrose.githubtoolbar.GitHubUrlParserTest"
```

### A note on the test task

`build.gradle.kts` makes three changes to the `test` task. All are required — drop any one and the
suite stops running:

- It removes the IntelliJ Platform plugin's `IntelliJPlatformArgumentProvider`, which forces
  `-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader` onto the test JVM. Kotest's
  ClassGraph-based discovery cannot traverse that loader.
- It drops the platform's `testFramework.jar` from the test classpath. That jar registers a JUnit
  Platform `LauncherSessionListener` that needs JUnit 4, and Gradle 9.7 opens a launcher session, so
  the listener runs and the test worker dies before a single test does.
- It disables Kotest's classpath scanning, which otherwise walks the entire platform classpath and
  exhausts the test JVM heap.

These are plain unit tests that never start an IDE, so none of that machinery is needed. Adding
platform integration tests later would mean a separate source set that keeps it.

### Continuous integration

Every pull request runs ktlint, the tests, and the IntelliJ Plugin Verifier, and lints the workflow
files themselves with [actionlint](https://github.com/rhysd/actionlint) — the release workflow only
runs on a tag, so nothing else would catch a mistake in it before a release. Formatting and workflow
checks run first, so those failures come back in seconds rather than after a full IDE download.

Built with the [IntelliJ Platform Gradle Plugin](https://github.com/JetBrains/intellij-platform-gradle-plugin).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full text.
