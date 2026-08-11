# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An IntelliJ Platform plugin that adds a main-toolbar button opening the current project's GitHub
repository page in the default browser. See `README.md` for user-facing behavior.

## Commands

A `Makefile` wraps these (`make help` lists targets): `build`, `tests`, `test-one TEST=<class>`,
`clean`, `run`, `dist`, `verify`, `check`, `versions`, `check-wrapper`, `upgrade-wrapper`, `all`.
Note the target is `tests`, not `test`. Underlying Gradle tasks:

```bash
./gradlew build              # compile + test
./gradlew test               # tests only
./gradlew runIde             # sandbox IDE with the plugin loaded
./gradlew buildPlugin        # installable ZIP -> build/distributions/
./gradlew verifyPlugin       # IntelliJ Plugin Verifier compatibility check
./gradlew dependencyUpdates  # newer stable releases -> build/dependencyUpdates/report.txt

./gradlew test --tests "com.pambrose.githubtoolbar.GitHubUrlParserTest"   # single test class
```

`dependencyUpdates` (ben-manes Gradle Versions Plugin) filters out pre-release candidates via
`isNonStable` in `build.gradle.kts`; drop that filter only if you actually want EAP/RC noise. It
cannot resolve `idea:ideaIC` — the IntelliJ artifact repositories expose no metadata it can query,
so the `intellijIdea` catalog entry has to be bumped by hand. The `java-compiler-ant-tasks` line in
the report tracks the same IDE branch and is a usable proxy for "a newer IDE line exists".

The Gradle wrapper is upgraded through the catalog: bump `gradle-wrapper` in
`gradle/libs.versions.toml`, then `make upgrade-wrapper`, which runs `./gradlew wrapper` twice
(Gradle's documented procedure — the first run rewrites `gradle-wrapper.properties` with the old
wrapper jar, the second regenerates the wrapper itself). Nothing in the build resolves that catalog
entry, so it is documentation for the Makefile's `sed`, not a dependency; `dependencyUpdates`
reports Gradle upgrades in its own "Gradle release-candidate updates" section instead. Because
nothing resolves it, the catalog and `gradle-wrapper.properties` can drift silently — `make check`
runs `check-wrapper` first to catch that.

## CI

`.github/workflows/build.yml` runs on pushes to `master`, on PRs, and on demand: wrapper-jar
validation, `make check-wrapper`, `./gradlew build`, `verifyPlugin`, and it uploads the packaged
ZIP as an artifact. `release.yml` fires on a `MAJOR.MINOR.PATCH` tag (no `v` prefix, matching the
release convention) and refuses to publish if the tag disagrees with `version` in
`gradle.properties` — otherwise a `1.0.1` release would ship a ZIP built as `1.0.0`.

Runs are slow on a cold cache: the build downloads a full IntelliJ IDEA distribution, and
`verifyPlugin` fetches its own IDEs on top of that. Both jobs cap at 45 minutes and PR runs cancel
their predecessors, because a private repository bills Actions minutes.

Every `uses:` is pinned to a 40-character commit SHA with the version in a trailing comment — a
mutable `@v6` tag can be repointed at new code, and `release.yml` holds `contents: write`. Don't
"tidy" these back into tags. `.github/dependabot.yml` updates the SHA and the comment together.

## Architecture

Three units in `com.pambrose.githubtoolbar`, split so the non-trivial logic carries no IntelliJ
dependency and stays directly unit-testable:

- **`GitHubUrlParser`** — pure Kotlin, zero platform imports. Normalizes any Git remote form
  (https, scp-style `git@`, `ssh://`, `git://`) to `https://github.com/owner/repo`. Strips embedded
  credentials so tokens never reach the browser. Host matching is *exact* against `github.com` /
  `www.github.com`, which is what rejects lookalikes like `evilgithub.com` and
  `github.com.evil.example` — don't loosen this to a `contains`/`endsWith` check.
- **`GitHubRepoLocator`** — wraps git4idea. Selects the Git root owning the current file (innermost
  wins when roots nest, via longest matching path), falling back to the project's first root; then
  prefers the `origin` remote, falling back to the first remote that parses as GitHub. Also produces
  the disabled-state tooltip text. Root containment compares whole path segments, so `/code/alpha`
  does not swallow `/code/alpha-two` — the same class of bug as the exact-host rule above.
- **`OpenOnGitHubAction`** — thin `AnAction`. Must return `ActionUpdateThread.BGT`, because
  `update()` reads Git repository state and that is not allowed on the EDT. `update()` also pins
  `isVisible = true` unconditionally and varies only `isEnabled`/`description`, so the button never
  vacates its toolbar slot and neighbouring icons never shift between projects.

`src/main/resources/META-INF/plugin.xml` registers the action into **both** toolbars. The group IDs
differ in casing and this is easy to get wrong: new UI is `MainToolbarLeft` (lowercase *b*), classic
UI is `MainToolBar` (capital *B*). Both were confirmed against `idea/PlatformActions.xml` in an
installed IDE.

Anchors are not intuitive, because the platform's own widgets already occupy these groups.
`MainToolbarLeft` holds `main.toolbar.Project` then `MainToolbarGeneralActionsGroup`, and the Git
plugin inserts the branch widget `before MainToolbarGeneralActionsGroup` — so `anchor="last"` lands
*after* the branch widget, at the right edge of the left cluster, not next to the project name. Use
`anchor="first"` for the true leftmost slot. Likewise in `MainToolbarRight`, the run-configuration
widget (`NewUiRunWidget`) is itself registered `anchor="first"`, so `first` there means *left of the
run widget* and `last` means right of the settings gear.

## Test task configuration

`tasks.test` removes the IntelliJ Platform plugin's `IntelliJPlatformArgumentProvider`, drops
`testFramework.jar` from the classpath, and disables Kotest classpath scanning. All three are
load-bearing — removing any one breaks the suite:

- The provider forces `-Djava.system.class.loader=com.intellij.util.lang.PathClassLoader`, which
  Kotest's ClassGraph discovery cannot traverse (`TestEngine with ID 'kotest' failed to discover
  tests`).
- The platform's `testFramework.jar` registers a JUnit Platform `LauncherSessionListener`
  (`JUnit5TestEnvironmentInitializer`) that requires JUnit 4. Gradle 9.7 opens a launcher session,
  so the listener runs and the worker dies before any test does (`Could not start Gradle Test
  Executor`, `NoClassDefFoundError: org/junit/rules/TestRule`). Gradle 9.6 did not hit this.
- Kotest's autoscan walks the entire platform classpath and exhausts the heap
  (`OutOfMemoryError` inside ClassGraph).

These are plain unit tests that never boot an IDE. Platform integration tests would need their own
source set that keeps the provider.

## Conventions

- Tests use Kotest `StringSpec()` with an `init {}` block, plus MockK. git4idea types
  (`GitRepository`, `GitRemote`) are mocked directly — `GitRemote` is `final`, which MockK handles.
- `gradle.properties` sets `kotlin.stdlib.default.dependency=false`; the platform ships its own
  kotlin-stdlib and adding Gradle's risks a version clash.
- `group` and `version` live in `gradle.properties`, not `build.gradle.kts`. An assignment in the
  build script silently overrides the property, so don't reintroduce one.
- `gradle/libs.versions.toml` also holds two versions Gradle never resolves as dependencies —
  `jvm` (read by `jvmToolchain`) and `gradle-wrapper` (read only by the Makefile's `sed`). Neither
  appears in `dependencyUpdates`.
- `since-build` is `252` with an open-ended `until-build`.
- Every `.kt` file opens with the Apache 2.0 header (the boilerplate from the `LICENSE` appendix,
  verbatim) before the `package` line; new sources need it too. The `.kts` build scripts do not
  carry one.
