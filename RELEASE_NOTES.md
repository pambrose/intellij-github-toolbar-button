<!--
  The body of the GitHub release for the version being prepared, replaced each time one goes out.
  `release.yml` passes this file as the release body and appends GitHub's generated commit list.

  This is prose for humans reading the release page — what changed, and anything worth knowing
  before upgrading. It is NOT the source of the plugin's `changeNotes`: those are generated from
  CHANGELOG.md by the org.jetbrains.changelog plugin. The cumulative history lives there too.
-->

# v1.1.0

Adds four more places to jump to on GitHub, and support for self-hosted GitHub Enterprise.

## More than the home page

A **GitHub** submenu now appears in the VCS Operations popup (<kbd>Ctrl</kbd>/<kbd>Cmd</kbd>+<kbd>V</kbd>)
and the Project View right-click menu, opening the repository's **Pull Requests**, **Issues**,
**Actions**, and **Releases** as well as its home page.

Each is a separate command, so any of them can be given its own keyboard shortcut under
**Settings → Keymap**. Right-clicking a file picks the Git root that owns it, which matters in a
project with nested roots.

The toolbar button itself is unchanged: one click, repository home page.

## GitHub Enterprise

Self-hosted installs are now supported. Add your host under **Settings → Tools → GitHub Toolbar
Button**, one per line — pasting a full clone URL works too, since only the host is kept.

Configured hosts are matched **exactly**, the same way `github.com` always has been. Adding
`github.mycompany.com` recognizes that host and nothing else: not `evilgithub.mycompany.com`, and
not `github.mycompany.com.evil.example`. The allowlist widens which hosts are accepted without
weakening how any of them is checked. `github.com` stays recognized no matter what you configure, so
a mistake in that field can never take the default away.

Enterprise repositories keep their own host in the opened URL —
`https://github.mycompany.com/owner/repo`.

## Before you upgrade

- **The command is now called "Open Repository on GitHub."** It was "Open on GitHub", which collides
  with the bundled GitHub plugin's own differently-behaving action of that name — that one opens the
  current *file* at the current revision. Its action ID is unchanged, so **existing keyboard
  shortcuts and toolbar customizations keep working**; only the name you search for has changed.
- **In menus, the command now hides** when the project has no GitHub repository, instead of
  appearing permanently greyed out. On a toolbar it still stays visible but disabled, so the button
  never vacates its slot and neighbouring icons never shift between projects.

## Installation

Download `intellij-github-toolbar-button-1.1.0.zip` from the release assets, then in IntelliJ IDEA
open **Settings → Plugins → ⚙ → Install Plugin from Disk…**, select the ZIP, and restart when
prompted.

Requires IntelliJ IDEA 2025.2 or later with the bundled **Git** plugin enabled.

**Full Changelog**: https://github.com/pambrose/intellij-github-toolbar-button/compare/1.0.0...1.1.0
