<!--
  The body of the GitHub release for the version being prepared, replaced each time one goes out.
  `release.yml` passes this file as the release body and appends GitHub's generated commit list.

  This is prose for humans reading the release page — what changed, and anything worth knowing
  before upgrading. It is NOT the source of the plugin's `changeNotes`: those are generated from
  CHANGELOG.md by the org.jetbrains.changelog plugin. The cumulative history lives there too.
-->

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

Install from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/33486-github-toolbar-button)
under **Settings → Plugins → Marketplace**, or download
`intellij-github-toolbar-button-1.1.1.zip` from the assets below and use **Settings → Plugins → ⚙ →
Install Plugin from Disk…**, restarting when prompted.

Requires IntelliJ IDEA 2025.2 or later with the bundled **Git** plugin enabled.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.1.0...1.1.1
