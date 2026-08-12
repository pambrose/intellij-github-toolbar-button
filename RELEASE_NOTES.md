<!--
  The prose bodies of this project's GitHub releases, newest first, one `# vMAJOR.MINOR.PATCH`
  section each. `release.yml` publishes the single section matching the tag being released and
  fails if there is none, so the heading has to read exactly `# v` followed by the tag.

  Write the new section at the top before tagging. Everything below it is the published record of
  a release that has already gone out — leave it alone.

  This is prose for humans reading a release page: what changed, and anything worth knowing before
  upgrading. It is NOT the source of the plugin's `changeNotes`, which are generated from
  CHANGELOG.md by the org.jetbrains.changelog plugin. GitHub appends its own generated commit list
  below whichever section is published.
-->

# v1.2.0

The commands are now on the menu bar, under **Git**. Everything else is where it was.

## The commands were missing from the main menu

Since 1.1.0 the plugin has offered more than its toolbar button — **Pull Requests**, **Issues**,
**Actions**, **Releases**, the branch commands, **Copy GitHub URL**. All of it was reachable from
the VCS Operations popup (<kbd>Ctrl</kbd>/<kbd>Cmd</kbd>+<kbd>V</kbd>) and by right-clicking in the
Project View, and nowhere else. If you went looking on the menu bar, you found nothing, and there
was no clue that the popup held more.

They now appear under **Git** in the main menu as well, as an **Open on GitHub** submenu, listing
exactly what the other two menus list.

### Why it is not called "GitHub"

The **Git** menu already contains a **GitHub** submenu — **Create Pull Request**, **View Pull
Requests**, **Create Gist**, and so on. That one belongs to the GitHub integration bundled with your
IDE, not to this plugin, and it does different things. Two adjacent submenus both reading *GitHub*
would be impossible to tell apart, so this plugin's takes the name **Open on GitHub** in that menu.
It is still titled **GitHub** in the VCS Operations popup and the Project View menu, where nothing
collides with it.

## It stays out of the way in projects it cannot help with

In a project with no Git repository, no remote, or a remote that is not on a recognized GitHub host,
the **Open on GitHub** submenu hides rather than sitting in the **Git** menu greyed out. That is what
these commands already did in context menus; the main menu now matches.

The toolbar button still behaves the other way on purpose: it greys out and keeps its slot, so the
icons beside it do not shift around as you move between projects.

Every command remains listed in **Find Action** and in **Settings → Keymap** in all cases, including
projects where it is momentarily unavailable — so you can always bind a shortcut to one.

## Before you upgrade

Nothing to do. No settings, action IDs, or keyboard shortcuts have changed, so existing keymap
bindings and toolbar customizations keep working. The new submenu is added to the **Git** menu; if
you have customized that menu under **Settings → Appearance & Behavior → Menus and Toolbars**, it
appears alongside the bundled GitHub submenu and can be moved or removed there like anything else.

## Installation

The plugin is on the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33486-github-toolbar-button). Open
**Settings → Plugins → Marketplace**, search for **GitHub Toolbar Button**, and click **Install** —
or **Update**, if you already have it, since Marketplace installs pick up new versions through the
IDE's own plugin updates.

If you installed a ZIP by hand previously, this release is a good moment to switch: uninstall that
copy and install from the Marketplace, and updates stop being something you have to do yourself.
The ZIP is still attached below for anyone who prefers it — **Settings → Plugins → ⚙ → Install
Plugin from Disk…**, then restart when prompted.

Requires IntelliJ IDEA 2025.2 or later with the bundled **Git** plugin enabled.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.1.1...1.2.0

# v1.1.1

A maintenance release. Three fixes, all of them cases where a command looked available and then did
the wrong thing — or nothing at all. No new features, and nothing to change in how you use it.

## Branch commands opened pages that did not exist

**Open Current Branch on GitHub** and **Open Pull Request for Current Branch** used your *local*
branch name. That is usually the same name the branch has on the remote, but not always:

```
git checkout -b fix origin/main
```

leaves a local branch `fix` tracking `origin/main`. Both commands saw a tracked upstream, enabled
themselves, and opened `…/tree/fix` — a 404, because `fix` had never been pushed.
`git push -u origin HEAD:release-2026` and `git branch --set-upstream-to` produced the same
mismatch.

They now open the name the branch carries on the remote, which is the one that exists. Where the
two names already agree — the ordinary case — nothing changes.

## Commands silently did nothing while the IDE was indexing

Nothing in this plugin reads an index, but nothing said so, and the platform will refuse an action
that has not declared it. The result was worse than a greyed-out menu: the entries stayed **enabled**
and simply did nothing when clicked, from the VCS Operations popup, the Project View context menu, a
keyboard shortcut, or Find Action.

The toolbar button was unaffected — it reaches the action by a different path that carries no such
check — so the button worked while every menu entry refused, which is why this went unnoticed for so
long. Indexing runs right after you open a project, switch branches, or sync a build, which is
exactly when this plugin is most useful.

Every command is now usable throughout.

## An empty GitHub submenu was left behind

In a project with no GitHub remote, the individual commands hid themselves from context menus, as
intended — but the **GitHub ▸** submenu holding them stayed, permanently greyed out. It now hides
along with its contents.

## Before you upgrade

Nothing to do. No settings, action IDs, or keyboard shortcuts have changed, so existing keymap
bindings and toolbar customizations keep working.

## Installation

The plugin is on the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33486-github-toolbar-button). Open
**Settings → Plugins → Marketplace**, search for **GitHub Toolbar Button**, and click **Install** —
or **Update**, if you already have it, since Marketplace installs pick up new versions through the
IDE's own plugin updates.

If you installed a ZIP by hand previously, this release is a good moment to switch: uninstall that
copy and install from the Marketplace, and updates stop being something you have to do yourself.
The ZIP is still attached below for anyone who prefers it — **Settings → Plugins → ⚙ → Install
Plugin from Disk…**, then restart when prompted.

Requires IntelliJ IDEA 2025.2 or later with the bundled **Git** plugin enabled.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.1.0...1.1.1

# v1.1.0

Everything in this release is reachable from a new **GitHub** submenu, in the VCS Operations popup
(<kbd>Ctrl</kbd>/<kbd>Cmd</kbd>+<kbd>V</kbd>) and the Project View right-click menu. The toolbar
button is unchanged: one click, repository home page.

## Go somewhere other than the home page

**Pull Requests**, **Issues**, **Actions**, and **Releases** for the same repository. Each is a
separate command, so any of them can take its own keyboard shortcut under **Settings → Keymap**.

## Go straight from a branch to its pull request

**Open Pull Request for Current Branch** opens GitHub's create-PR page for the branch you are on —
push, then jump straight to opening the PR. **Open Current Branch on GitHub** opens its tree view.

Both stay disabled until the branch tracks an upstream, because a branch that has never been pushed
has no page on GitHub. The tooltip says which applies: whether you are on a detached HEAD, or simply
have not pushed yet.

## Copy instead of open

**Copy GitHub URL** puts the repository URL on the clipboard rather than opening a browser, for
pasting into a review or a chat message. It resolves the URL exactly as the button does.

## Forks can reach upstream

When a repository has more than one GitHub remote, an **Open Other Remote** submenu lists them all.
Previously `origin` always won, which left a fork's `upstream` unreachable. The button still follows
`origin`; this adds a path to the others rather than making the choice ambiguous.

Repositories with a single remote — almost all of them — see nothing new.

## GitHub Enterprise

Self-hosted installs are now supported. Add your host under **Settings → Tools → GitHub Toolbar
Button**, one per line; pasting a full clone URL works too, since only the host is kept.

Configured hosts are matched **exactly**, the same way `github.com` always has been. Adding
`github.mycompany.com` recognizes that host and nothing else: not `evilgithub.mycompany.com`, and
not `github.mycompany.com.evil.example`. The allowlist widens which hosts are accepted without
weakening how any of them is checked, and `github.com` stays recognized no matter what you
configure, so a mistake in that field can never take the default away.

Enterprise repositories keep their own host in the opened URL —
`https://github.mycompany.com/owner/repo`.

## Before you upgrade

- **The command is now called "Open Repository on GitHub."** It was "Open on GitHub", which collides
  with the bundled GitHub plugin's own differently-behaving action of that name — that one opens the
  current *file* at the current revision. Its action ID is unchanged, so **existing keyboard
  shortcuts and toolbar customizations keep working**; only the name you search for has changed.
- **In menus, commands now hide** when the project has no GitHub repository, instead of appearing
  permanently greyed out. On a toolbar the button still stays visible but disabled, so it never
  vacates its slot and neighbouring icons never shift between projects.

## Installation

Download `intellij-github-toolbar-button-1.1.0.zip` from the release assets, then in IntelliJ IDEA
open **Settings → Plugins → ⚙ → Install Plugin from Disk…**, select the ZIP, and restart when
prompted.

Requires IntelliJ IDEA 2025.2 or later with the bundled **Git** plugin enabled.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.0.0...1.1.0

# v1.0.0

First release of GitHub Toolbar Button — an IntelliJ IDEA plugin that adds a main-toolbar button opening the current project's GitHub repository page in your default browser.

## What it does

- **One click to the repository home page.** Opens `https://github.com/owner/repo` — not a branch or file URL — so it works even when the current branch has never been pushed. IntelliJ's bundled GitHub integration can open the current file at the current revision from a context menu; this is the simpler, more direct path.
- **Always visible.** The button holds its toolbar slot in every project, so neighbouring icons never shift around.
- **Disabled when it can't help.** With no Git repository, no remote, or a non-GitHub remote, the button greys out and its tooltip says which. It never fails silently and never opens an unexpected dialog.

## Which remote it uses

`origin` wins when a project has several remotes; otherwise the first remote resolving to a GitHub URL. When a project has several Git roots, the root owning the currently-open file wins — innermost first when roots nest — falling back to the project's first root.

Every common remote form is recognized: `https://`, scp-style `git@github.com:owner/repo.git`, `ssh://`, and `git://`.

## Security

Credentials embedded in a remote URL (`https://user:token@github.com/...`) are stripped before the URL reaches your browser. Host matching is exact against `github.com` / `www.github.com`, so lookalikes such as `evilgithub.com` or `github.com.evil.example` are rejected rather than opened.

Only `github.com` is treated as GitHub — self-hosted GitHub Enterprise remotes leave the button disabled.

## Requirements

- IntelliJ IDEA 2025.2 or later (Community or Ultimate), with an open-ended upper bound
- The bundled **Git** plugin enabled

## Installation

Download `intellij-github-toolbar-button-1.0.0.zip` below, then in IntelliJ IDEA open **Settings → Plugins → ⚙ → Install Plugin from Disk…**, select the ZIP, and restart when prompted.

## Also in this release

Apache 2.0 licensing, and CI covering build, tests, and the IntelliJ Plugin Verifier, plus this release automation. Every workflow action is pinned to a commit SHA.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/commits/1.0.0
