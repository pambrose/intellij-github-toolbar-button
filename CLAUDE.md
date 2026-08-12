# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An IntelliJ Platform plugin that adds a main-toolbar button opening the current project's GitHub
repository page in the default browser. See `README.md` for user-facing behavior.

## Commands

A `Makefile` wraps these (`make help` lists targets): `build`, `tests`, `test-one TEST=<class>`,
`clean`, `run`, `dist`, `verify`, `check`, `coverage`, `lint`, `format`, `versions`,
`check-wrapper`, `upgrade-wrapper`, `all`. Note the target is `tests`, not `test`. Underlying
Gradle tasks:

```bash
./gradlew build              # compile + test
./gradlew test               # tests only
./gradlew runIde             # sandbox IDE with the plugin loaded
./gradlew buildPlugin        # installable ZIP -> build/distributions/
./gradlew verifyPlugin       # IntelliJ Plugin Verifier compatibility check
./gradlew dependencyUpdates  # newer stable releases -> build/dependencyUpdates/report.txt
./gradlew koverHtmlReport    # coverage report -> build/reports/kover/html/index.html

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

`release.yml` runs `./gradlew build` before anything else, and that step is load-bearing: neither
`buildPlugin` nor `verifyPlugin` depends on `test`, so without it a tag could ship to the GitHub
release *and* the Marketplace with the suite never run. A tag does not have to sit on a commit that
was ever on a green `master`.

`build.gradle.kts` attaches `verifyPluginProjectConfiguration` and `verifyPluginStructure` to
`check`; the IntelliJ Platform plugin registers both and wires neither, so otherwise they never run.
Treat them as diagnostics, not gates — both report and **neither fails the build**. In particular
`verifyPluginStructure` does *not* notice a missing `META-INF/pluginIcon.svg` (measured, by deleting
it), so nothing local protects the icons below.

Runs are slow on a cold cache: the build downloads a full IntelliJ IDEA distribution, and
`verifyPlugin` fetches its own IDEs on top of that. Both jobs cap at 45 minutes, and PR runs cancel
their predecessors so a superseded commit does not hold a runner.

Every `uses:` is pinned to a 40-character commit SHA with the version in a trailing comment — a
mutable `@v6` tag can be repointed at new code, and `release.yml` holds `contents: write`. Don't
"tidy" these back into tags. `.github/dependabot.yml` updates the SHA and the comment together.

`release.yml` only ever runs on a tag, so nothing else exercises it between releases and a mistake in
it surfaces at the worst moment. `build.yml` therefore runs
[actionlint](https://github.com/rhysd/actionlint) over both workflows on every PR. It already caught
one bug here — the `secrets` context is **not** available in a step-level `if`, which is why
credential presence is turned into a step *output* and tested through that instead.

actionlint is downloaded by pinned version and checked against a SHA-256 rather than pulled through
a third-party action or an install script piped into a shell, which would undercut the point of
SHA-pinning every `uses:` above it. Bumping the version means bumping the checksum with it; both
live in that step's `env`.

### Marketplace publishing

Publishing is opt-in and skips cleanly when unconfigured, so tags keep producing GitHub releases
either way. It needs four repository secrets — `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`,
`PRIVATE_KEY_PASSWORD` — read from the environment in `build.gradle.kts` so they never enter the
repository. With them absent, `signPlugin` reports `SKIPPED` and no `*-signed.zip` is produced.

`META-INF/pluginIcon.svg` (and its `_dark` twin) must exist and be 40×40 — Marketplace rejects a
plugin without one. They are deliberately *not* the Octocat: that mark is GitHub's.

The mark is a frame with an arrow leaving it, and the colour is on the arrow rather than the frame:
the frame is what you are in, the arrow is what happens. Only the frame stroke differs between the
two files (`#3C3F41` light, `#CED0D6` dark) — the amber `#D97706` is shared, because it clears the
3:1 contrast floor for graphics on both grounds (3.19 on white, 4.33 on `#2B2D30`). Judge any
replacement at 40×40, not enlarged: that is the size the Marketplace lists, and detail that survives
at 80&nbsp;px routinely turns to mush at 40.

The first upload of a plugin has to be made by hand through the Marketplace UI; the API only accepts
updates to a listing that already exists.

## Formatting

ktlint runs through the Kotlinter plugin: `make lint` reports, `make format` fixes what it can, and
`make check` lints before testing. CI runs `lintKotlin` ahead of the build so a formatting failure
comes back in seconds instead of after a full IDE download.

**`.editorconfig` is load-bearing and must stay tracked.** ktlint takes its rules from it, so if it
were untracked CI would lint with stock defaults and fail on violations the checked-in config turns
off — `indent`, `import-ordering`, `no-trailing-spaces` and the wrapping rules among them.

`function-signature` and `class-signature` are *on*, so any declaration with two or more parameters
is written across multiple lines:

```kotlin
fun urlFor(
    repoUrl: String,
    branch: String,
): String?
```

That is the house style here — match it in new code rather than reaching for the exclusion, and note
that `make format` will rewrite a single-line signature for you.

## Changelog and release notes

`CHANGELOG.md` is the single source for the plugin's `changeNotes`: the `org.jetbrains.changelog`
plugin extracts the section matching `version` from `gradle.properties` (falling back to
`[Unreleased]`) and converts it to the HTML the plugin manager renders. Don't hand-write
`changeNotes` — it will be overwritten. `RELEASE_NOTES.md` is separate and hand-written: the prose
for one release, replaced each time.

Two settings are load-bearing:

- `versionPrefix = ""`, because tags here carry no `v`. The default writes links like
  `compare/v1.0.0...HEAD`, which 404 against this repository's tags.
- `introduction` duplicates `CHANGELOG.md`'s preamble. `patchChangelog` rewrites the file and only
  preserves a preamble it knows about, so the two must be edited together.

Release flow: bump `version` in `gradle.properties`, then `make patch-changelog`, which moves the
`[Unreleased]` notes into a dated section. Bump *first* — run against a version that already has a
section and `patchChangelog` consumes the notes and writes nothing, silently losing them. `make
patch-changelog` guards against exactly that; `./gradlew patchChangelog` does not. Note it stamps the
local date, which can differ from the UTC date the releases page shows.

## Architecture

Everything lives in `com.pambrose.githubtoolbar`, split so the non-trivial logic carries no IntelliJ
dependency and stays directly unit-testable:

- **`GitHubUrlParser`** — pure Kotlin, zero platform imports. Normalizes any Git remote form
  (https, scp-style `git@`, `ssh://`, `git://`) to `https://host/owner/repo`. Strips embedded
  credentials so tokens never reach the browser. Host matching is *exact* against the caller's
  `allowedHosts` (defaulting to `DEFAULT_HOSTS`: `github.com` / `www.github.com`), which is what
  rejects lookalikes like `evilgithub.com` and `github.com.evil.example` — don't loosen this to a
  `contains`/`endsWith` check. GitHub Enterprise is supported by *widening that set*, never by
  weakening the comparison. `normalizeHost` reduces user-entered text to a bare hostname, and
  `normalizeHosts` reduces a block of typed lines to exactly what the settings store — the settings
  page and the store must derive that list identically, so it is written once here.
- **`GitHubHostSettings` / `GitHubHostConfigurable`** — an application-level
  `SimplePersistentStateComponent` holding the extra enterprise hosts, plus its Settings → Tools
  page. `allowedHosts` always unions in `DEFAULT_HOSTS`, so a bad entry can only fail to add a host,
  never remove github.com. Mutations go through `State.replaceHosts` because marking state dirty
  needs `BaseState.incrementModificationCount`, which is `protected`; the public
  `intIncrementModificationCount` is `@ApiStatus.Internal` and **fails `make verify`**. Both the
  store and the page reduce typed input through `GitHubUrlParser.normalizeHosts`, and that has to
  stay a single function: `isModified` compares what was typed against what was stored, so two
  derivations that disagree leave Apply either permanently enabled or permanently dead.
- **`GitHubRepoLocator`** — wraps git4idea. Selects the Git root owning the current file (innermost
  wins when roots nest, via longest matching path), falling back to the project's first root; then
  prefers the `origin` remote, falling back to the first remote that parses as GitHub. Also produces
  the disabled-state tooltip text. Root containment compares whole path segments, so `/code/alpha`
  does not swallow `/code/alpha-two` — the same class of bug as the exact-host rule above. It takes
  `allowedHosts` as a *parameter* and never reads `GitHubHostSettings` itself: a service lookup here
  needs a running Application, which plain unit tests do not have, and reintroducing one fails the
  whole suite with `ApplicationManager.getApplication()` returning null. Only the action layer reads
  settings.
- **`GitHubDestination`** — pure Kotlin enum of the pages the plugin can open (repository home,
  `/pulls`, `/issues`, `/actions`, `/releases`). It only appends a fixed path to a URL
  `GitHubUrlParser` already validated, so it inherits the exact-host and credential-stripping
  guarantees. Never let it parse or re-derive a host, or those guarantees stop holding.
- **`GitHubBranchDestination`** — pure Kotlin enum for the branch-specific pages (`/tree/<branch>`,
  `/pull/new/<branch>`). Separate from `GitHubDestination` because these need a branch and that one
  does not. It percent-encodes the branch for the URL but leaves `/` intact: a slash is ordinary in
  a branch name *and* is GitHub's path separator, so `feature/x` must stay `feature/x`. Git permits
  `#` and `?` in a ref name, and either would otherwise change where the URL ends.
- **`OpenGitHubBranchAction`** — the branch counterpart of `OpenGitHubDestinationAction`. Stays
  disabled unless `GitHubRepoLocator.pushedBranchOf` returns a branch, which it only does when the
  branch tracks an upstream: a branch that was never pushed 404s on GitHub. Don't relax this to
  "any current branch" — the point is not opening a dead page.
- **`GitHubRemotesActionGroup`** — the one dynamic group here: its children are built per
  invocation because the remotes are unknown until a project is open, so they cannot be declared in
  `plugin.xml`. `GitHubRepoLocator.gitHubRemotes` supplies them, `origin` first. The group hides
  itself below two remotes, since offering a choice of one is noise. Everything else in the plugin
  resolves a *single* URL preferring `origin`; this exists because that silently hides `upstream` in
  a fork.
- **`GitHubDestinationsGroup`** — the class behind the `GitHub` submenu, and it exists for one
  line: `templatePresentation.isHideGroupIfEmpty = true`. The children hide themselves in a context
  menu, but the platform's default for an emptied *popup group* is to grey it out and keep it
  (`isDisableGroupIfEmpty` is on by default, `isHideGroupIfEmpty` is not), which leaves behind
  exactly the dead entry the children avoid. Set the flag from the constructor: overriding
  `createTemplatePresentation()`, the way `DefaultCompactActionGroup` does, is `@ApiStatus.Internal`
  and **fails `make verify`** with `INTERNAL_API_USAGES` — verified, not assumed. `compact="true"`
  is not a substitute either; it only hides *disabled* children.
- **`CopyGitHubUrlAction`** — same resolution as `OpenOnGitHubAction`, but writes to the clipboard
  via `CopyPasteManager.copyTextToClipboard` instead of opening a browser.
- **`GitHubUrlAction`** — the base every URL-producing action extends, and **the one place the
  enabled/visible/tooltip policy is written**. It was previously copied into each action and
  explained in only one of them. `update()` and `actionPerformed()` are `final`; subclasses supply
  `urlFor`, `describe` and `perform`, and override `unavailableReason` only where a more specific
  disabled-state tooltip exists (which only `OpenGitHubBranchAction` does).
  `update()` reads Git repository state, which is not allowed on the EDT — hence `BGT`. Visibility
  is place-dependent: on a toolbar the button stays visible but disabled, so it never vacates its
  slot and neighbouring icons never shift between projects; in a context menu (`e.isFromContextMenu`)
  it hides instead, because a permanently dead menu entry is only clutter. Use `isFromContextMenu`,
  not `ActionPlaces.isPopupPlace` — the Plugin Verifier flags the latter as deprecated, and
  `make verify` reports it.
  The `GitHubHostSettings` lookup lives here rather than in `GitHubRepoLocator`, which is what keeps
  that object free of any need for a running Application. Subclasses keep no-argument constructors,
  because the platform instantiates them reflectively.
- **`OpenGitHubDestinationAction`** — binds a `GitHubDestination` to `GitHubUrlAction`, and nothing
  more. Its subclasses (`OpenOnGitHubAction` and the four in `GitHubDestinationActions.kt`) exist
  *only* to bind a destination: the platform can call only a no-argument constructor, so the
  destination cannot be a registration attribute.

**Everything registered in `plugin.xml` implements `DumbAware`**, and `PluginRegistrationTest`
enforces it by walking the XML. Nothing here reads an index, so nothing should be refused while one
is being built — and the failure is nastier than it sounds. `ActionUtil.performAction` carries the
dumb-mode guard, so the menu, keymap and Find Action paths refuse a non-dumb-aware action, while a
toolbar `ActionButton` does not go through it and keeps working. `update()` meanwhile still reports
the action as enabled, because it reads only git4idea state. The result is an action that looks
available, works from the toolbar, and silently does nothing everywhere else. Marking only a group
is worse than marking nothing: its submenu opens and lists entries that then refuse the click.

`src/main/resources/META-INF/plugin.xml` registers the action into **both** toolbars plus two menus
(`Vcs.Operations.Popup`, `ProjectViewPopupMenu`). The toolbar group IDs differ in casing and this is
easy to get wrong: new UI is `MainToolbarLeft` (lowercase *b*), classic UI is `MainToolBar` (capital
*B*). Every group ID here was confirmed to exist in an installed IDE — a typo'd group is not an
error, the action simply never appears.

The action is titled **"Open Repository on GitHub"** deliberately. The bundled GitHub plugin's
`Github.Open.In.Browser` group presents itself as "Open on GitHub" in Find Action (via
`<override-text place="GoToAction"/>`), and it does something different — current file at the
current revision. Don't rename this back.

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

## Coverage

Kover, through `make coverage` (HTML report plus a printed total) and `koverVerify`, which the
plugin attaches to `check` itself — so `make check` and CI's `./gradlew build` both enforce it with
no wiring of our own. **Unlike `verifyPluginStructure` above, this one really does fail the build**:
raising the bound above the actual figure exits 1 with `Rule violated: lines covered percentage
is …`.

**Pin `kover` by hand, and do not trust `dependencyUpdates` here.** It reads Maven Central, which
still advertises **0.9.1** as newest; 0.9.1 cannot configure against Kotlin 2.4 at all, failing with
`Could not get unknown property 'compileKotlinTask'`. The Gradle Plugin Portal carries the newer
line, and **0.9.9** is what works.

Coverage is measured over the layer the unit suite can reach. The exclusion patterns in
`build.gradle.kts` drop the action, group and configurable classes, which need a running
Application — the same boundary that keeps `GitHubRepoLocator` free of a settings lookup. Measuring
everything would report roughly half, and that number would fall every time an action was added
without anything being tested less well. **Each pattern needs a trailing `*`**: nested and anonymous
classes are named `Outer$inner`, so a pattern ending at `Group` cannot match
`GitHubRemotesActionGroup$getChildren$1$1`.

Two things worth knowing about what the bound of 90 (against ~97% actual) does and does not catch.
It catches untested *new* logic well — the measured layer is only about 100 lines, so eight
uncovered additions breach it. It is a poor detector of *deleted tests*: removing the whole of
`GitHubUrlParserTest` moves coverage only from 97.2% to 96.2%, because the locator and settings
specs exercise the parser thoroughly on their way past. Do not read a green `koverVerify` as
evidence that the specs are still there.

The uncovered remainder inside the measured layer is exactly the platform boundary —
`locate(Project, …)` and `getInstance() = service()`, both of which need a running Application.

## Conventions

- Tests use Kotest `StringSpec()` with an `init {}` block, plus MockK. git4idea types
  (`GitRepository`, `GitRemote`) are mocked directly — `GitRemote` is `final`, which MockK handles.
- `gradle.properties` sets `kotlin.stdlib.default.dependency=false`; the platform ships its own
  kotlin-stdlib and adding Gradle's risks a version clash.
- `group` and `version` live in `gradle.properties`, not `build.gradle.kts`. An assignment in the
  build script silently overrides the property, so don't reintroduce one.
- `gradle/libs.versions.toml` also holds two versions Gradle never resolves as dependencies —
  `jvm` (read by `jvmToolchain`) and `gradle-wrapper` (read only by the Makefile's `sed`). Neither
  appears in `dependencyUpdates`. The README's version badges read this file live over
  `raw.githubusercontent.com` (shields.io `dynamic/toml`), so renaming a key silently breaks a
  badge — but no badge can fall out of date.
- `since-build` is `252` with an open-ended `until-build`.
- Every `.kt` file opens with the Apache 2.0 header (the boilerplate from the `LICENSE` appendix,
  verbatim) before the `package` line; new sources need it too. The `.kts` build scripts do not
  carry one.
